package org.hzontal.shared_ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.databinding.DualTextCheckLayoutBinding

class DualTextCheckView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @StringRes
    private var rightTextRes: Int = -1

    @StringRes
    private var leftTextRes: Int = -1

    private var isCheckboxVisible: Boolean = false


    private val binding: DualTextCheckLayoutBinding =
        DualTextCheckLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        this.onCheckedChangeListener = listener
    }


    init {
        initListeners()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.DualTextCheckView, defStyleAttr, defStyleAttr
            )

            try {
                rightTextRes = typedArray.getResourceId(R.styleable.DualTextCheckView_rightText, -1)
                leftTextRes = typedArray.getResourceId(R.styleable.DualTextCheckView_leftText, -1)
                isCheckboxVisible =
                    typedArray.getBoolean(R.styleable.DualTextCheckView_checkboxVisible, false)

            } finally {
                typedArray.recycle()
            }
        }
        bindView()
    }

    private fun bindView() {
        if (rightTextRes != -1) {
            binding.rightTextView.setText(rightTextRes)
        }
        if (leftTextRes != -1) {
            binding.leftTextView.setText(leftTextRes)
        }
        binding.checkBox.isVisible = isCheckboxVisible
    }


    private fun initListeners() {
        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChangeListener?.invoke(isChecked)
        }
    }

    fun setRightText(text : String?){
        if (text != null){
            binding.rightTextView.text = text
        }
    }

}