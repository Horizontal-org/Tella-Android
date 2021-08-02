package org.hzontal.shared_ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import org.hzontal.shared_ui.R

class HomeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    @StringRes
    private var titleText: Int = -1

    @DrawableRes
    private var rightImg: Int = -1
    private lateinit var titleTextView: TextView
    private lateinit var rightImgView: AppCompatImageView
    private lateinit var rootV: View
    var clickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.home_button, this, true)
        initView()
        initListeners()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        titleTextView = findViewById(R.id.textTitle)
        rootV = findViewById(R.id.root)
        rightImgView = findViewById(R.id.img)
    }


    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.HomeJavaButton,
                    defStyleAttr,
                    defStyleAttr
                )

            try {
                titleText = typedArray.getResourceId(R.styleable.HomeJavaButton_mainTitleText, -1)
                rightImg = typedArray.getResourceId(R.styleable.HomeJavaButton_rightImg, -1)

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
           titleTextView.setText(titleText)
           rightImgView.setBackgroundResource(rightImg)
    }
}