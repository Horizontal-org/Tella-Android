package org.hzontal.shared_ui.textviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.databinding.LayoutCenterTextviewBinding

class CenterMessageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @StringRes
    private var text: Int = -1

    @DrawableRes
    private var topImg: Int = -1
    private val binding: LayoutCenterTextviewBinding =
        LayoutCenterTextviewBinding.inflate(LayoutInflater.from(context), this, true)


    init {
        extractAttributes(attrs, defStyleAttr)
    }


    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.CenterMessageTextView,
                    defStyleAttr,
                    defStyleAttr
                )

            try {
                text = typedArray.getResourceId(R.styleable.CenterMessageTextView_textMessage, -1)
                topImg = typedArray.getResourceId(R.styleable.CenterMessageTextView_topImg, -1)

            } finally {
                typedArray.recycle()
            }
        }
        bindView()
    }


    private fun bindView() {
        setTextAndVisibility(text, binding.textviewDescription)
        if (topImg != -1) {
            binding.imgTop.setBackgroundResource(topImg)
        }
    }

    private fun setTextAndVisibility(text: Int, textView: TextView) {
        if (text != -1) {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(text)
        }
    }

    fun setText(text: String?) {
        binding.textviewDescription.text = text
    }

    fun setTopIcon(icon: Int) {
        binding.imgTop.setImageDrawable(AppCompatResources.getDrawable(context, icon))
    }

}