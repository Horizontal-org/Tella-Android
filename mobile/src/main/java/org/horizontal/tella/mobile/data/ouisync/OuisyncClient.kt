package org.horizontal.tella.mobile.data.ouisync

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.webkit.MimeTypeMap
import com.hzontal.tella_vault.BaseVault
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.database.VaultDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.equalitie.ouisync.service.Service
import org.equalitie.ouisync.session.AccessChange
import org.equalitie.ouisync.session.AccessMode
import org.equalitie.ouisync.session.EntryType
import org.equalitie.ouisync.session.LocalSecret
import org.equalitie.ouisync.session.OuisyncException
import org.equalitie.ouisync.session.Repository
import org.equalitie.ouisync.session.Session
import org.equalitie.ouisync.session.SecretKey
import org.equalitie.ouisync.session.SetLocalSecret
import org.equalitie.ouisync.session.close
import org.equalitie.ouisync.session.create
import org.equalitie.ouisync.session.subscribe
import org.horizontal.tella.mobile.MyApplication
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class OuisyncRepo(val path: String, val name: String)

data class OuisyncEntry(val name: String, val path: String, val directory: Boolean)

enum class OuisyncSyncState { WaitingForContent, Downloading, UpToDate }

data class OuisyncListing(val entries: List<OuisyncEntry>, val syncState: OuisyncSyncState) {
    val syncing: Boolean get() = syncState != OuisyncSyncState.UpToDate
}

enum class OuisyncSaveResult { NotSynced, Saved }

object OuisyncClient {
    private val mutex = Mutex()
    private var service: Service? = null
    private var session: Session? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var networkReady = false
    private val clients = AtomicInteger()

    fun retain() {
        clients.incrementAndGet()
    }

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                Build.SUPPORTED_ABIS.any { it in setOf("armeabi-v7a", "arm64-v8a", "x86_64") }

    suspend fun repositories(context: Context): List<OuisyncRepo> = io {
        val current = session(context)
        ensureNetwork(context, current)
        current.listRepositories().map { (path, repo) ->
            enableSync(repo)
            OuisyncRepo(
                path,
                repo.getShortName().ifBlank { path.substringAfterLast('/') }
            )
        }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    suspend fun importRepository(context: Context, link: String) = io {
        val current = session(context)
        val token = current.validateShareToken(normalizeLink(link))
        val suggestedName = current.getShareTokenSuggestedName(token).trim()
        val baseName = suggestedName
            .map { if (it == '/' || it == '\\' || it.isISOControl()) '_' else it }
            .joinToString("")
            .takeIf { it.isNotBlank() && it != "." && it != ".." }
            ?: "Repository"
        val repo = withLocalReadKey(context, current) { readKey ->
            current.createRepository(
                path = baseName,
                readSecret = SetLocalSecret.KeyAndSalt(readKey, current.generatePasswordSalt()),
                writeSecret = null,
                token = token,
                syncEnabled = true,
                dhtEnabled = false,
                pexEnabled = false
            )
        }
        enableSync(repo)
    }

    suspend fun updates(context: Context, repoName: String): Flow<Unit> = io {
        val current = session(context)
        ensureNetwork(context, current)
        repository(current, repoName).also { enableSync(it) }.subscribe()
    }

    suspend fun entries(context: Context, repoName: String, path: String): OuisyncListing = io {
        val current = session(context)
        ensureNetwork(context, current)
        val repo = repository(current, repoName)
        enableSync(repo)
        val entries = readEntries(repo, path).sortedWith(
            compareBy<OuisyncEntry> { !it.directory }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
        val progress = repo.getSyncProgress()
        OuisyncListing(entries, ouisyncSyncState(progress.value, progress.total))
    }

    private suspend fun repository(current: Session, path: String): Repository =
        current.listRepositories()[path] ?: throw OuisyncException.NotFound()

    private suspend fun enableSync(repo: Repository) {
        if (!repo.isSyncEnabled()) repo.setSyncEnabled(true)
        if (repo.isDhtEnabled()) repo.setDhtEnabled(false)
        if (repo.isPexEnabled()) repo.setPexEnabled(false)
    }

    private suspend fun readEntries(repo: Repository, path: String): List<OuisyncEntry> =
        repo.readDirectory(path.ifEmpty { "/" }).map { entry ->
            OuisyncEntry(
                entry.name,
                childPath(path, entry.name),
                entry.entryType == EntryType.DIRECTORY
            )
        }

    private fun normalizeLink(raw: String): String {
        val link = raw.replace(Regex("\\s+"), "").replace("%23", "#", ignoreCase = true)
        val at = link.indexOf("https://ouisync.net/r#")
        return if (at >= 0) link.substring(at) else link
    }

    private fun childPath(parent: String, name: String): String = "${parent.trimEnd('/')}/$name"

    suspend fun saveToVault(
        context: Context,
        repoName: String,
        path: String,
        fileName: String,
        directory: Boolean
    ): OuisyncSaveResult = io {
        val repo = repository(session(context), repoName)
        if (directory) {
            val progress = repo.getSyncProgress()
            if (ouisyncSyncState(progress.value, progress.total) != OuisyncSyncState.UpToDate) {
                return@io OuisyncSaveResult.NotSynced
            }
        }
        val vault = MyApplication.keyRxVault.rxVault.blockingFirst()
        val outputs = mutableMapOf<String, BaseVault.VaultOutputStream>()
        val source = object : OuisyncTreeImporter.Source {
            override suspend fun entries(path: String) = readEntries(repo, path)

            override suspend fun open(path: String): OuisyncTreeImporter.Input {
                val file = repo.openFile(path)
                try {
                    val length = file.getLength()
                    val available = file.getProgress()
                    return object : OuisyncTreeImporter.Input {
                        override val length = length
                        override val available = available
                        override suspend fun read(offset: Long, size: Long) =
                            file.read(offset, size)

                        override suspend fun close() = file.close()
                    }
                } catch (error: Throwable) {
                    withContext(NonCancellable) {
                        try {
                            file.close()
                        } catch (closeError: Throwable) {
                            error.addSuppressed(closeError)
                        }
                    }
                    throw error
                }
            }
        }
        val destination = object : OuisyncTreeImporter.Destination {
            override fun create(name: String, directory: Boolean, parentId: String): String {
                val builder = vault.builder().setName(name)
                    .setType(if (directory) VaultFile.Type.DIRECTORY else VaultFile.Type.FILE)
                if (!directory) {
                    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    builder.setMimeType(
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                            ?: "application/octet-stream"
                    )
                }
                return builder.build(parentId).blockingGet().id
            }

            override fun output(id: String): BaseVault.VaultOutputStream =
                vault.getOutStream(vault.get(id).blockingGet()).also { outputs[id] = it }

            override fun complete(id: String) {
                checkNotNull(outputs.remove(id)).complete(0L)
            }

            override fun delete(id: String) {
                check(vault.delete(vault.get(id).blockingGet()).blockingGet()) {
                    "Could not remove incomplete Ouisync import"
                }
            }
        }
        try {
            OuisyncTreeImporter(source, destination).copy(
                path, fileName, directory, VaultDataSource.ROOT_UID
            )
            OuisyncSaveResult.Saved
        } catch (error: OuisyncNotSyncedException) {
            if (error.suppressed.isNotEmpty()) throw error
            OuisyncSaveResult.NotSynced
        }
    }

    private suspend fun session(context: Context): Session {
        check(isSupported) { "Ouisync is not supported on this device" }
        session?.let { return it }
        val app = context.applicationContext
        val ouisyncDir = File(app.filesDir, "TellaOuisync")
        val configDir = File(ouisyncDir, "config").apply {
            check(mkdirs() || isDirectory) { "Could not create Ouisync config directory" }
        }.absolutePath
        val storeDir = File(ouisyncDir, "store").apply {
            check(mkdirs() || isDirectory) { "Could not create Ouisync store directory" }
        }.absolutePath
        var created: Session? = null
        try {
            if (service == null) service = Service.start(configDir)
            val current = Session.create(configDir)
            created = current
            current.setStoreDirs(listOf(storeDir))
            withLocalReadKey(app, current) { readKey ->
                for ((_, repo) in current.listRepositories()) {
                    prepareRepository(current, repo, readKey)
                }
            }
            ensureNetwork(app, current)
            session = current
            return current
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                try {
                    created?.close()
                } catch (closeError: Throwable) {
                    error.addSuppressed(closeError)
                }
                try {
                    service?.stop()
                } catch (stopError: Throwable) {
                    error.addSuppressed(stopError)
                }
                service = null
                networkReady = false
                releaseMulticastLock()
            }
            throw error
        }
    }

    suspend fun release() = withContext(NonCancellable + Dispatchers.IO) {
        mutex.withLock {
            if (clients.decrementAndGet() > 0) return@withLock
            try {
                session?.close()
            } finally {
                session = null
                networkReady = false
                try {
                    service?.stop()
                } finally {
                    service = null
                    releaseMulticastLock()
                }
            }
        }
    }

    private suspend fun prepareRepository(
        current: Session,
        repo: Repository,
        readKey: SecretKey
    ) {
        val needsProtection = repo.getAccessMode() != AccessMode.BLIND
        repo.setAccessMode(AccessMode.READ, LocalSecret.SecretKey(readKey))
        if (repo.getAccessMode() != AccessMode.READ) throw OuisyncException.PermissionDenied()
        repo.setAccess(
            read = if (needsProtection) {
                AccessChange.Enable(
                    SetLocalSecret.KeyAndSalt(
                        readKey,
                        current.generatePasswordSalt()
                    )
                )
            } else null,
            write = AccessChange.Disable
        )
    }

    private suspend fun <T> withLocalReadKey(
        context: Context,
        current: Session,
        block: suspend (SecretKey) -> T
    ): T {
        val readKey =
            SecretKey(OuisyncLocalKeyStore(context.applicationContext).loadOrCreate(current))
        try {
            currentCoroutineContext().ensureActive()
            return block(readKey)
        } finally {
            readKey.value.fill(0)
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }

    private suspend fun ensureNetwork(context: Context, session: Session) {
        acquireMulticastLock(context)
        if (!networkReady) {
            startNetwork(session)
            session.setLocalDiscoveryEnabled(true)
            session.setPortForwardingEnabled(false)
            networkReady = true
        }
    }

    private fun acquireMulticastLock(context: Context) {
        if (multicastLock?.isHeld == true) return
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return
        multicastLock = wifi.createMulticastLock("ouisync").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private suspend fun startNetwork(session: Session) {
        try {
            session.bindNetwork(DUAL_STACK_BIND)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d("ouisync dual-stack bind failed: ${error.javaClass.simpleName}")
            try {
                session.bindNetwork(IPV4_BIND)
            } catch (ipv4Error: CancellationException) {
                throw ipv4Error
            } catch (ipv4Error: Exception) {
                Timber.d("ouisync tcp bind failed: ${ipv4Error.javaClass.simpleName}")
                session.bindNetwork(listOf("quic/0.0.0.0:0"))
            }
        }
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { block() }
    }
}

private val DUAL_STACK_BIND = listOf("quic/0.0.0.0:0", "quic/[::]:0", "tcp/0.0.0.0:0", "tcp/[::]:0")
private val IPV4_BIND = listOf("quic/0.0.0.0:0", "tcp/0.0.0.0:0")

internal fun ouisyncSyncState(downloaded: Long, total: Long): OuisyncSyncState = when {
    total == 0L -> OuisyncSyncState.WaitingForContent
    downloaded < total -> OuisyncSyncState.Downloading
    else -> OuisyncSyncState.UpToDate
}

