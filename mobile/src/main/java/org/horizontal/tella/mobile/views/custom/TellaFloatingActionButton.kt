package org.horizontal.tella.mobile.views.custom

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.horizontal.tella.mobile.R

class TellaFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.floatingActionButtonStyle
) : FloatingActionButton(context, attrs, defStyleAttr) {

    init {
        applyDefaults(attrs, defStyleAttr)
    }

    private fun applyDefaults(attrs: AttributeSet?, defStyleAttr: Int) {
        val sizePx = (FAB_SIZE_DP * resources.displayMetrics.density).toInt()
        val maxImagePx = (MAX_IMAGE_SIZE_DP * resources.displayMetrics.density).toInt()

        setCustomSize(sizePx)
        setMaxImageSize(maxImagePx)
        useCompatPadding = true

        var iconRes = 0
        var backgroundTint = ContextCompat.getColor(context, R.color.dark_orange)
        var iconTint = ContextCompat.getColor(context, R.color.wa_white)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.TellaFloatingActionButton,
                defStyleAttr,
                0
            )
            try {
                iconRes = typedArray.getResourceId(
                    R.styleable.TellaFloatingActionButton_fabIcon,
                    0
                )
                backgroundTint = typedArray.getColor(
                    R.styleable.TellaFloatingActionButton_fabBackgroundTint,
                    backgroundTint
                )
                iconTint = typedArray.getColor(
                    R.styleable.TellaFloatingActionButton_fabIconTint,
                    iconTint
                )
            } finally {
                typedArray.recycle()
            }
        }

        backgroundTintList = ColorStateList.valueOf(backgroundTint)
        imageTintList = ColorStateList.valueOf(iconTint)
        if (iconRes != 0) {
            setImageResource(iconRes)
        }
    }

    fun setFabIcon(@DrawableRes iconRes: Int) {
        setImageResource(iconRes)
    }

    companion object {
        private const val FAB_SIZE_DP = 50
        private const val MAX_IMAGE_SIZE_DP = 30
    }
}
