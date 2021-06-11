package com.hzontal.shared_ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.hzontal.shared_ui.R


class InformationButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable {

    @StringRes
    private var topText : Int = -1
    @StringRes
    private var topTextCenter : Int = -1
    @StringRes
    private var bottomTextCenter : Int = -1
    @StringRes
    private var bottomText : Int = -1
    @DrawableRes
    private var startImg : Int = -1
    private lateinit var centerTopTextView: TextView
    private lateinit var startTopTextView: TextView
    private lateinit var centerBottomTextView: TextView
    private lateinit var startBottomTextView: TextView
    private lateinit var startImageView: ImageView
    private lateinit var rootV : View
    var clickListener: (() -> Unit)? = null
    private var mChecked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.information_button, this, true)
        initView()
        initListeners()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        centerTopTextView = findViewById(R.id.centerTopTv)
        startTopTextView = findViewById(R.id.startTopTv)
        centerBottomTextView = findViewById(R.id.centerBottomTv)
        startBottomTextView = findViewById(R.id.startBottomTv)
        rootV = findViewById(R.id.root)
        startImageView = findViewById(R.id.leftImg)
        background = ContextCompat.getDrawable(context,R.drawable.bg_information_button)
    }


    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                    .obtainStyledAttributes(
                            attrs,
                            R.styleable.InformationButton,
                            defStyleAttr,
                            defStyleAttr
                    )

            try {
                topText = typedArray.getResourceId(R.styleable.InformationButton_topText, -1)
                topTextCenter = typedArray.getResourceId(R.styleable.InformationButton_topTextCenter, -1)
                bottomTextCenter = typedArray.getResourceId(R.styleable.InformationButton_bottomTextCenter, -1)
                bottomText = typedArray.getResourceId(R.styleable.InformationButton_bottomText, -1)
                startImg = typedArray.getResourceId(R.styleable.InformationButton_startImg, -1)
                mChecked = typedArray.getBoolean(R.styleable.InformationButton_state_checked, false)
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
        setTextAndVisibility(topText,startTopTextView)
        setTextAndVisibility(topTextCenter,centerTopTextView)
        setTextAndVisibility(bottomTextCenter,centerBottomTextView)
        setTextAndVisibility(bottomText,startBottomTextView)
        if (startImg != -1){
            startImageView.visibility = View.VISIBLE
            startImageView.setBackgroundResource(startImg)
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