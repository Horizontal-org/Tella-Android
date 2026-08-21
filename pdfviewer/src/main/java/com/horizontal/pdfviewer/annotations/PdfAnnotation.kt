package com.horizontal.pdfviewer.annotations

import org.json.JSONObject

/**
 * A single user-created annotation on a PDF page.
 *
 * Coordinates are stored as page-relative fractions (0.0 .. 1.0) so they
 * scale correctly regardless of the device width, zoom level, or rendered
 * bitmap size used by [com.horizontal.pdfviewer.PdfRendererCore].
 *
 * - For [Type.HIGHLIGHT], `x/y/width/height` describe the highlighted rectangle.
 * - For [Type.STICKY_NOTE], `x/y` describe the anchor point of the note marker
 *   (the small icon the user taps to open the note). `width/height` are kept
 *   for layout consistency but are typically the marker size.
 */
data class PdfAnnotation(
    val id: String,
    val page: Int,
    val type: Type,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String,
    val color: Int,
    val createdAt: Long,
    val updatedAt: Long
) {

    enum class Type(val raw: String) {
        HIGHLIGHT("highlight"),
        STICKY_NOTE("sticky_note");

        companion object {
            fun fromRaw(raw: String?): Type? = entries.firstOrNull { it.raw == raw }
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_PAGE, page)
        put(KEY_TYPE, type.raw)
        put(KEY_X, x)
        put(KEY_Y, y)
        put(KEY_WIDTH, width)
        put(KEY_HEIGHT, height)
        put(KEY_TEXT, text)
        put(KEY_COLOR, color)
        put(KEY_CREATED_AT, createdAt)
        put(KEY_UPDATED_AT, updatedAt)
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_PAGE = "page"
        private const val KEY_TYPE = "type"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_TEXT = "text"
        private const val KEY_COLOR = "color"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"

        fun fromJson(json: JSONObject): PdfAnnotation? = try {
            val type = Type.fromRaw(json.optString(KEY_TYPE))
            if (type == null) null
            else PdfAnnotation(
                id = json.optString(KEY_ID),
                page = json.optInt(KEY_PAGE),
                type = type,
                x = json.optDouble(KEY_X).toFloat(),
                y = json.optDouble(KEY_Y).toFloat(),
                width = json.optDouble(KEY_WIDTH).toFloat(),
                height = json.optDouble(KEY_HEIGHT).toFloat(),
                text = json.optString(KEY_TEXT),
                color = json.optInt(KEY_COLOR),
                createdAt = json.optLong(KEY_CREATED_AT),
                updatedAt = json.optLong(KEY_UPDATED_AT)
            )
        } catch (t: Throwable) {
            null
        }
    }
}
