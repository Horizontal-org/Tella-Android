package org.hzontal.shared_ui.switches

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.hzontal.shared_ui.R


class TellaSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable{

    @StringRes
    private var titleText : Int = -1
    @StringRes
    private var explainText : Int = -1
    @DrawableRes
    private var endImage : Int = -1
    @DrawableRes
    private var startImage : Int = -1

    private lateinit var titleTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var titleStartIcon: ImageView
    private lateinit var titleEndIcon: ImageView
    private lateinit var mSwitch: SwitchCompat
    private lateinit var rootV : View
    var clickListener: (() -> Unit)? = null
    private var mChecked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.switch_with_text, this, true)
        initView()
        initListeners()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        titleTextView = findViewById(R.id.titleTV)
        messageTextView = findViewById(R.id.explainTV)
        titleStartIcon = findViewById(R.id.startImage)
        titleEndIcon = findViewById(R.id.endImage)
        mSwitch = findViewById(R.id.mSwitch)

        rootV = findViewById(R.id.root)

        background = ContextCompat.getDrawable(context,R.drawable.bg_information_button)
    }


    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.TellaSwitch,
                    defStyleAttr,
                    defStyleAttr
                )

            try {
                titleText = typedArray.getResourceId(R.styleable.TellaSwitch_titleText, -1)
                explainText = typedArray.getResourceId(R.styleable.TellaSwitch_explainText, -1)
                startImage = typedArray.getResourceId(R.styleable.TellaSwitch_startImage, -1)
                endImage = typedArray.getResourceId(R.styleable.TellaSwitch_endImage, -1)
                mChecked = typedArray.getBoolean(R.styleable.TellaSwitch_is_checked, false)
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
        rootV.setOnClickListener {
            clickListener?.invoke()
        }
    }

    private fun bindView() {
        setTextAndVisibility(titleText,titleTextView)
        setTextAndVisibility(explainText,messageTextView)
        if (startImage != -1){
            titleStartIcon.visibility = View.VISIBLE
            titleStartIcon.setBackgroundResource(startImage)
        }
        if (endImage != -1){
            titleEndIcon.visibility = View.VISIBLE
            titleEndIcon.setBackgroundResource(endImage)
        }

    }

    private fun setTextAndVisibility(text : Int,textView: TextView){
        if (text != -1) {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(text)
        }
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
            refreshLayoutState();
        }
    }

    private fun refreshLayoutState() {
        super.refreshDrawableState()
        background = if (mChecked)
            ContextCompat.getDrawable(context,R.drawable.bg_information_button_selected)
        else
            ContextCompat.getDrawable(context,R.drawable.bg_information_button)
    }

}