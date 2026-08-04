package org.hzontal.shared_ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.hzontal.shared_ui.R

class OnboardingInfoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val messageView: TextView

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_information_button)
        val padding = resources.getDimensionPixelSize(R.dimen.onboarding_info_card_padding)
        setPadding(padding, padding, padding, padding)
        inflate(context, R.layout.onboarding_info_card, this)
        messageView = findViewById(R.id.info_card_message)

        context.obtainStyledAttributes(attrs, R.styleable.OnboardingInfoCard, defStyleAttr, 0).apply {
            try {
                val messageRes = getResourceId(R.styleable.OnboardingInfoCard_infoMessage, -1)
                if (messageRes != -1) {
                    setMessage(messageRes)
                }
            } finally {
                recycle()
            }
        }
    }

    fun setMessage(@StringRes messageRes: Int) {
        messageView.setText(messageRes)
    }

    fun setMessage(message: CharSequence) {
        messageView.text = message
    }
}
