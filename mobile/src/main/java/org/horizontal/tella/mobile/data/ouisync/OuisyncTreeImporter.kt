package org.horizontal.tella.mobile.data.ouisync

import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class OuisyncNotSyncedException : IOException("Ouisync file is not fully available")

internal class OuisyncTreeImporter(
    private val source: Source,
    private val destination: Destination
) {
    interface Source {
        suspend fun entries(path: String): List<OuisyncEntry>
        suspend fun open(path: String): Input
    }

    interface Input {
        val length: Long
        val available: Long
        suspend fun read(offset: Long, size: Long): ByteArray
        suspend fun close()
    }

    interface Destination {
        fun create(name: String, directory: Boolean, parentId: String): String
        fun output(id: String): OutputStream
        fun complete(id: String)

        fun delete(id: String)
    }

    suspend fun copy(path: String, name: String, directory: Boolean, parentId: String) {
        val createdIds = mutableListOf<String>()
        try {
            currentCoroutineContext().ensureActive()
            val id = destination.create(name, directory, parentId)
            createdIds.add(id)
            copyContents(path, directory, id, createdIds)
            currentCoroutineContext().ensureActive()
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                for (id in createdIds.asReversed()) {
                    try {
                        destination.delete(id)
                    } catch (cleanupFailure: Throwable) {
                        if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
                    }
                }
            }
            throw failure
        }
    }

    private suspend fun copyContents(
        path: String,
        directory: Boolean,
        id: String,
        createdIds: MutableList<String>
    ) {
        currentCoroutineContext().ensureActive()
        if (directory) {
            for (entry in source.entries(path)) {
                currentCoroutineContext().ensureActive()
                val childId = destination.create(entry.name, entry.directory, id)
                createdIds.add(childId)
                copyContents(entry.path, entry.directory, childId, createdIds)
            }
        } else {
            copyFile(path, id)
        }
    }

    private suspend fun copyFile(path: String, id: String) {
        val input = source.open(path)
        var failure: Throwable? = null
        try {
            val length = input.length
            if (length < 0) throw IOException("Invalid Ouisync file length")
            if (input.available < length) throw OuisyncNotSyncedException()
            currentCoroutineContext().ensureActive()
            destination.output(id).use { output ->
                var offset = 0L
                while (offset < length) {
                    currentCoroutineContext().ensureActive()
                    val requested = minOf(CHUNK_SIZE, length - offset)
                    val bytes = input.read(offset, requested)
                    currentCoroutineContext().ensureActive()
                    if (bytes.isEmpty()) throw OuisyncNotSyncedException()
                    if (bytes.size.toLong() > requested) {
                        throw IOException("Ouisync returned more bytes than requested")
                    }
                    output.write(bytes)
                    offset += bytes.size
                }
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            try {
                withContext(NonCancellable) { input.close() }
            } catch (closeFailure: Throwable) {
                val original = failure
                if (original == null) throw closeFailure
                if (closeFailure !== original) original.addSuppressed(closeFailure)
            }
        }
        currentCoroutineContext().ensureActive()
        destination.complete(id)
    }

    private companion object {
        const val CHUNK_SIZE = 256L * 1024L
    }
}
