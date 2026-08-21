package com.horizontal.pdfviewer.annotations

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistent store for [PdfAnnotation]s keyed by a stable file identifier
 * (typically the VaultFile id passed in from the host app).
 *
 * Storage format: a single JSON file per document inside the application's
 * private files directory. The file contains a JSON array of annotations.
 * This keeps the implementation dependency-free (only uses `org.json` shipped
 * with the Android framework) and survives app upgrades and reboots.
 *
 * All public methods are thread-safe (synchronized on the internal monitor)
 * and safe to call from the main thread — the JSON files are tiny (a few KB
 * at most for normal usage) so disk I/O is essentially instant.
 */
class PdfAnnotationStore private constructor(
    private val context: Context,
    private val fileId: String
) {

    private val lock = Any()

    private val file: java.io.File by lazy {
        java.io.File(context.filesDir, ANNOT_DIR).apply { mkdirs() }
            .resolve("$fileId.json")
    }

    val annotations: List<PdfAnnotation>
        get() = synchronized(lock) { loadAllInternal() }

    fun annotationsForPage(page: Int): List<PdfAnnotation> =
        synchronized(lock) { loadAllInternal().filter { it.page == page } }

    fun add(annotation: PdfAnnotation): PdfAnnotation = synchronized(lock) {
        val list = loadAllInternal().toMutableList()
        val finalized = if (annotation.id.isBlank()) {
            annotation.copy(id = UUID.randomUUID().toString())
        } else annotation
        list.add(finalized)
        saveAllInternal(list)
        finalized
    }

    fun update(annotation: PdfAnnotation) = synchronized(lock) {
        val list = loadAllInternal().toMutableList()
        val idx = list.indexOfFirst { it.id == annotation.id }
        if (idx >= 0) {
            list[idx] = annotation.copy(updatedAt = System.currentTimeMillis())
        } else {
            // not found — treat as add to avoid silent data loss
            list.add(annotation.copy(updatedAt = System.currentTimeMillis()))
        }
        saveAllInternal(list)
    }

    fun delete(id: String) = synchronized(lock) {
        val list = loadAllInternal().toMutableList()
        val removed = list.removeAll { it.id == id }
        if (removed) saveAllInternal(list)
        removed
    }

    fun clearAll() = synchronized(lock) {
        if (file.exists()) file.delete()
    }

    fun count(): Int = synchronized(lock) { loadAllInternal().size }

    // ---- internal helpers (callers MUST hold `lock`) ----

    private fun loadAllInternal(): List<PdfAnnotation> {
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                PdfAnnotation.fromJson(obj)
            }
        } catch (t: Throwable) {
            // Corrupt file — fail safe, return empty list rather than crash.
            emptyList()
        }
    }

    private fun saveAllInternal(list: List<PdfAnnotation>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        } catch (t: Throwable) {
            // Persisting is best-effort; we must not crash the UI thread
            // if the device is out of space or the file is locked.
        }
    }

    companion object {
        private const val ANNOT_DIR = "pdf_annotations"

        @Volatile private var instance: PdfAnnotationStore? = null

        fun get(context: Context, fileId: String): PdfAnnotationStore {
            val ctx = context.applicationContext
            return if (instance?.fileId == fileId) {
                instance!!
            } else {
                synchronized(this) {
                    val cached = instance
                    if (cached != null && cached.fileId == fileId) cached
                    else PdfAnnotationStore(ctx, fileId).also { instance = it }
                }
            }
        }

        /**
         * Drops the cached instance for [fileId]. Useful when the host wants
         * to guarantee a fresh reload (e.g. after a bulk import operation).
         */
        fun reset(fileId: String) {
            synchronized(this) {
                if (instance?.fileId == fileId) instance = null
            }
        }

        /**
         * Renders a JSON snapshot of all annotations for [fileId]. Exposed for
         * the audit / export pipeline — not used by the viewer itself.
         */
        fun exportSnapshot(context: Context, fileId: String): String {
            return JSONArray().apply {
                get(context, fileId).annotations.forEach { put(it.toJson()) }
            }.toString()
        }

        /**
         * Internal helper for tests / audit: writes a JSONObject summary.
         */
        fun summarySnapshot(context: Context, fileId: String): JSONObject = JSONObject().apply {
            val store = get(context, fileId)
            put("file_id", fileId)
            put("count", store.count())
            put("highlights", store.annotations.count { it.type == PdfAnnotation.Type.HIGHLIGHT })
            put("sticky_notes", store.annotations.count { it.type == PdfAnnotation.Type.STICKY_NOTE })
        }
    }
}
