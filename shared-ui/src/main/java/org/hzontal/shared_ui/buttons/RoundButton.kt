package org.hzontal.shared_ui.buttons

import android.content.Context
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
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable {

    @StringRes
    private var text : Int = -1
    private val binding : LayoutRoundButtonBinding = LayoutRoundButtonBinding.inflate(LayoutInflater.from(context),this,true)
    private var mChecked = false
    var clickListener: (() -> Unit)? = null

    init {
        initView()
        initListeners()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        background = ContextCompat.getDrawable(context, R.drawable.bg_information_button)
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.RoundButton,
                    defStyleAttr,
                    defStyleAttr
                )

            try {
                text = typedArray.getResourceId(R.styleable.RoundButton_text, -1)
                mChecked = typedArray.getBoolean(R.styleable.RoundButton_check_state, false)
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
        setTextAndVisibility(text,binding.sheetTextView)
    }

    private fun setTextAndVisibility(text : Int,textView: TextView){
        if (text != -1) {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(text)
        }
    }

    fun setText(text : String?){
        binding.sheetTextView.text = text
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
        background = if (mChecked)
            ContextCompat.getDrawable(context, R.drawable.bg_information_button_selected)
        else
            ContextCompat.getDrawable(context, R.drawable.bg_information_button)
    }

}