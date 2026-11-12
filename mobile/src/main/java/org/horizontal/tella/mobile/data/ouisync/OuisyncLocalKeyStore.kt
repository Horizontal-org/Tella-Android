package org.horizontal.tella.mobile.data.ouisync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.equalitie.ouisync.session.Session
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class OuisyncLocalKeyStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "ouisync-local-keys"))

    suspend fun loadOrCreate(session: Session): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
        val wrappingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing) {
            val key = checkNotNull(wrappingKey) {
                "The Ouisync storage key is unavailable"
            }
            val envelope = file.readFully()
            val readKey = openOuisyncReadKey(envelope, key)
            try {
                if (envelope[0] != OUISYNC_READ_KEY_VERSION) save(readKey, key)
                return readKey
            } catch (error: Throwable) {
                readKey.fill(0)
                throw error
            }
        }

        val key = wrappingKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
        val readKey = session.generateSecretKey().value
        try {
            save(readKey, key)
            return readKey
        } catch (error: Throwable) {
            readKey.fill(0)
            throw error
        }
    }

    private fun save(readKey: ByteArray, wrappingKey: SecretKey) {
        val encrypted = sealOuisyncReadKey(readKey, wrappingKey)
        val output = file.startWrite()
        try {
            output.write(encrypted)
            output.fd.sync()
            file.finishWrite(output)
            check(file.readFully().contentEquals(encrypted)) { "Could not save the Ouisync read key" }
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    private companion object {
        const val KEY_ALIAS = "TellaOuisyncLocalKeys"
    }
}
