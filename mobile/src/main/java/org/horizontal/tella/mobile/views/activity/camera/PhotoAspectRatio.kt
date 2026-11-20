package org.horizontal.tella.mobile.views.activity.camera

import android.util.Rational
import android.util.Size
import androidx.annotation.StringRes
import org.horizontal.tella.mobile.R
import kotlin.math.roundToInt

enum class PhotoAspectRatio(
    @StringRes val titleRes: Int,
    private val ratioWidth: Int = 1,
    private val ratioHeight: Int = 1,
) {
    THREE_FOUR(R.string.camera_aspect_ratio_3_4, 3, 4),
    NINE_SIXTEEN(R.string.camera_aspect_ratio_9_16, 9, 16),
    ONE_ONE(R.string.camera_aspect_ratio_1_1, 1, 1),
    FULL(R.string.camera_aspect_ratio_full);

    fun sizeForWidth(width: Int) = Size(
        width,
        (width * ratioHeight.toDouble() / ratioWidth).roundToInt()
    )

    fun fitInside(maxWidth: Int, maxHeight: Int): Size {
        if (maxWidth <= 0 || maxHeight <= 0) return Size(0, 0)
        val widthOverHeight = ratioWidth.toDouble() / ratioHeight
        val fullWidthSize = sizeForWidth(maxWidth)
        return if (fullWidthSize.height <= maxHeight) {
            fullWidthSize
        } else {
            Size((maxHeight * widthOverHeight).roundToInt(), maxHeight)
        }
    }

    fun cropRatio(screenWidth: Int, screenHeight: Int) = if (this == FULL) {
        Rational(
            minOf(screenWidth, screenHeight).coerceAtLeast(1),
            maxOf(screenWidth, screenHeight).coerceAtLeast(1)
        )
    } else Rational(ratioWidth, ratioHeight)

    companion object {
        fun fromSavedValue(value: String?) = entries.firstOrNull { it.name == value } ?: THREE_FOUR
    }
}
