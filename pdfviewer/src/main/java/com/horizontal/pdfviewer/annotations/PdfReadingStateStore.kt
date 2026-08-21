package com.horizontal.pdfviewer.annotations

import android.content.Context

/**
 * Lightweight persistent storage for "where did I stop reading this PDF?".
 *
 * Stores, per file id, the last visible page index (0-based) and a coarse
 * vertical scroll offset (in pixels, normalized by screen density so it
 * survives rotation / different devices reasonably well — when the device
 * changes we fall back to just the page index which is enough for a good UX).
 *
 * Backed by SharedPreferences so reads/writes are O(1) and never block the
 * UI thread on a normal phone. This intentionally lives OUTSIDE the encrypted
 * Vault because:
 *  1. It contains no sensitive content (only an integer + a float),
 *  2. The host activity ([org.horizontal.tella.mobile.views.activity.viewer.PDFReaderActivity])
 *     already loads the PDF stream from the encrypted Vault — the position
 *     marker is just a UX hint, not protected data.
 *
 * If a future use case requires the position itself to be sensitive (e.g.
 * revealing which page of a sensitive report was last read), the host app
 * can wrap the returned integers with Vault encryption before calling back.
 */
class PdfReadingStateStore private constructor(
    private val prefs: android.content.SharedPreferences,
    val fileId: String
) {
    fun saveLastPage(pageIndex: Int, scrollOffsetPx: Int, density: Float) {
        val dpOffset = if (density > 0f) scrollOffsetPx / density else 0f
        prefs.edit()
            .putInt(keyPage(fileId), pageIndex)
            .putFloat(keyOffset(fileId), dpOffset)
            .putLong(keyTimestamp(fileId), System.currentTimeMillis())
            .apply()
    }

    fun getLastPage(): Int = prefs.getInt(keyPage(fileId), 0).coerceAtLeast(0)

    fun getLastScrollOffsetDp(): Float = prefs.getFloat(keyOffset(fileId), 0f)

    fun getLastSavedAt(): Long = prefs.getLong(keyTimestamp(fileId), 0L)

    fun clear() {
        prefs.edit()
            .remove(keyPage(fileId))
            .remove(keyOffset(fileId))
            .remove(keyTimestamp(fileId))
            .apply()
    }

    private fun keyPage(id: String) = "$KEY_PAGE_PREFIX$id"
    private fun keyOffset(id: String) = "$KEY_OFFSET_PREFIX$id"
    private fun keyTimestamp(id: String) = "$KEY_TS_PREFIX$id"

    companion object {
        private const val PREFS_NAME = "tella_pdf_reading_state_v1"
        private const val KEY_PAGE_PREFIX = "page:"
        private const val KEY_OFFSET_PREFIX = "offset_dp:"
        private const val KEY_TS_PREFIX = "ts:"

        @Volatile private var instance: PdfReadingStateStore? = null

        fun get(context: Context, fileId: String): PdfReadingStateStore {
            val ctx = context.applicationContext
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // 2025-08-19 (audit-fix): the previous version used identity
            // comparison (`===`) on the SharedPreferences instance to
            // decide whether the cached store was still valid. That works
            // because Android returns the SAME SharedPreferences instance
            // for the same name on the same Context, but it's brittle —
            // the docs only guarantee equality, not identity. Use the
            // name + fileId as the cache key instead.
            val cached = instance
            if (cached != null && cached.fileId == fileId) {
                return cached
            }
            return synchronized(this) {
                val inSyncCached = instance
                if (inSyncCached != null && inSyncCached.fileId == fileId) {
                    inSyncCached
                } else {
                    PdfReadingStateStore(prefs, fileId).also { instance = it }
                }
            }
        }
    }
}
