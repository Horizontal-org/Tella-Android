package org.hzontal.shared_ui.textviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.databinding.ViewTipsCardBinding

class TipsCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewTipsCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        context.withStyledAttributes(attrs, R.styleable.TipsCardView) {
            title = getString(R.styleable.TipsCardView_tipsTitle) ?: title
            description = getString(R.styleable.TipsCardView_tipsDescription) ?: description

            getResourceId(R.styleable.TipsCardView_tipsIcon, 0).takeIf { it != 0 }?.let { setIcon(it) }
            getColor(R.styleable.TipsCardView_tipsCardColor, 0).takeIf { it != 0 }?.let { setCardColor(it) }
            getColor(R.styleable.TipsCardView_tipsTitleColor, 0).takeIf { it != 0 }?.let { setTitleColor(it) }
            getColor(R.styleable.TipsCardView_tipsTextColor, 0).takeIf { it != 0 }?.let { setTextColor(it) }

        }
        isClickable = true
        isFocusable = true
    }

    var title: CharSequence
        get() = binding.tipsTitle.text
        set(value) { binding.tipsTitle.text = value }

    var description: CharSequence
        get() = binding.tipsDescription.text
        set(value) { binding.tipsDescription.text = value }

    fun setOnTipsClick(listener: OnClickListener) {
        binding.tipsIcon.setOnClickListener(listener)
    }

    private fun setIcon(@DrawableRes resId: Int) {
        binding.tipsIcon.setImageResource(resId)
    }

    private fun setCardColor(@ColorInt color: Int) {
        (binding.root as MaterialCardView).setCardBackgroundColor(color)
    }

    private fun setTitleColor(@ColorInt color: Int) {
        binding.tipsTitle.setTextColor(color)
    }

    fun setTextColor(@ColorInt color: Int) {
        binding.tipsDescription.setTextColor(color)
    }
}