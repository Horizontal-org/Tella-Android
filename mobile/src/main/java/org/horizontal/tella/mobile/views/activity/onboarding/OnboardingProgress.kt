package org.horizontal.tella.mobile.views.activity.onboarding

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.horizontal.tella.mobile.R

object OnboardingProgress {

    const val DOT_COUNT = 6
    const val LOCK_INDEX = 4
    const val ALL_DONE_INDEX = 5

    fun setup(container: LinearLayout, context: Context) {
        container.removeAllViews()
        val dotSize = context.resources.getDimensionPixelSize(R.dimen.onboarding_indicator_dot_size)
        val spacing = context.resources.getDimensionPixelSize(R.dimen.onboarding_indicator_spacing)
        for (i in 0 until DOT_COUNT) {
            val indicator = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginStart = if (i == 0) 0 else spacing
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.default_dot))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            container.addView(indicator)
        }
    }

    fun setActive(container: LinearLayout, context: Context, activeIndex: Int) {
        for (i in 0 until container.childCount) {
            val imageView = container.getChildAt(i) as ImageView
            val drawableRes = if (i == activeIndex) R.drawable.selected_dot else R.drawable.default_dot
            imageView.setImageDrawable(ContextCompat.getDrawable(context, drawableRes))
        }
    }
}
