package org.hzontal.shared_ui.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.databinding.LayoutRoundButtonBinding

class RoundButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable {

    @StringRes
    private var text: Int = -1
    private var tintColor: Int = -1
    private var textColor: Int = -1
    private var isTextAllCaps = false
    private val binding: LayoutRoundButtonBinding =
        LayoutRoundButtonBinding.inflate(LayoutInflater.from(context), this, true)
    private var mChecked = false
    private var isTextBold = false
    private var defaultBackground = R.drawable.bg_information_button
    var clickListener: (() -> Unit)? = null

    init {
        initListeners()
        extractAttributes(attrs, defStyleAttr)
        initView()
    }

    private fun initView() {
        binding.sheetTextView.background =
            ContextCompat.getDrawable(context, defaultBackground)
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.RoundButton, defStyleAttr, defStyleAttr
            )

            try {
                text = typedArray.getResourceId(R.styleable.RoundButton_text, -1)
                mChecked = typedArray.getBoolean(R.styleable.RoundButton_check_state, false)
                tintColor = typedArray.getColor(R.styleable.RoundButton_tint_color, -1)
                textColor = typedArray.getColor(R.styleable.RoundButton_text_color, -1)
                isTextAllCaps = typedArray.getBoolean(R.styleable.RoundButton_text_all_caps, true)
                isTextBold = typedArray.getBoolean(R.styleable.RoundButton_text_round_bold, false)
                defaultBackground = typedArray.getResourceId(
                    R.styleable.RoundButton_default_background,
                    R.drawable.bg_information_button
                )
                isChecked = mChecked

            } finally {
                typedArray.recycle()
            }
        }
        bindView()
    }

    fun setOnClickListener(cb: (() -> Unit)) {
        clickListener = cb
    }

    private fun initListeners() {
        binding.sheetTextView.setOnClickListener {
            clickListener?.invoke()
        }
    }

    private fun bindView() {
        setTextAndVisibility(text, binding.sheetTextView)
        setBackgroundTintColor(tintColor)
        setTextCaps(isTextAllCaps)
        setTextBold(isBold = isTextBold)
    }

    private fun setTextAndVisibility(text: Int, textView: TextView) {
        if (text != -1) {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(text)
        }
    }

    private fun setBackgroundTintColor(tintColor: Int) {
        if (tintColor != -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.sheetTextView.backgroundTintList = ColorStateList.valueOf(tintColor)
            }
        }
        if (textColor != -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.sheetTextView.setTextColor(textColor)
            }
        }
    }

    fun setText(text: String?) {
        binding.sheetTextView.text = text
    }

    fun setTextColor(color: Int) {
        binding.sheetTextView.setTextColor(color)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshLayoutState()
        }
    }

    private fun refreshLayoutState() {
        super.refreshDrawableState()
        binding.sheetTextView.background = if (mChecked) ContextCompat.getDrawable(
            context,
            R.drawable.bg_information_button_selected
        )
        else ContextCompat.getDrawable(context, defaultBackground)
    }

    private fun setTextCaps(isTextAllCaps: Boolean) {
        binding.sheetTextView.isAllCaps = isTextAllCaps
    }

    private fun setTextBold(isBold: Boolean) {
        binding.sheetTextView.setTypeface(
            null,
            if (isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

}