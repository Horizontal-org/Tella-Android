package org.hzontal.shared_ui.appbar


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import org.hzontal.shared_ui.R


class ToolbarComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    Toolbar(context, attrs, defStyleAttr) {

    private lateinit var btnBack: AppCompatImageButton
    private lateinit var toolbarTextView: TextView
    private lateinit var titleImg: ImageView
    private lateinit var startTitleTv: TextView
    private lateinit var endTitleTv: TextView

    @DrawableRes
    private var arrowBackIcon: Int = -1

    @DrawableRes
    private var titleIcon: Int = -1

    @StringRes
    var toolbarTitle: Int = -1

    var startTitle: Int = -1

    var endTitle: Int = -1

    var backClickListener: (() -> Unit)? = null

    var onEndClickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.component_toolbar, this, true)
        initView()
        initListener()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        btnBack = findViewById(R.id.btn_back)
        toolbarTextView = findViewById(R.id.titleTv)
        titleImg = findViewById(R.id.titleImg)
        startTitleTv = findViewById(R.id.startTitleTv)
        endTitleTv = findViewById(R.id.endTitleTv)

    }

    private fun initListener() {
        btnBack.setOnClickListener {
            backClickListener?.invoke()
        }
        endTitleTv.setOnClickListener {
            onEndClickListener?.invoke()
        }
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {

            val typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.ToolbarComponent, defStyleAttr, 0)

            try {
                toolbarTitle =
                    typedArray.getResourceId(R.styleable.ToolbarComponent_toolbarTitle, -1)
                arrowBackIcon =
                    typedArray.getResourceId(R.styleable.ToolbarComponent_arrowBackIcon, -1)
                titleIcon = typedArray.getResourceId(R.styleable.ToolbarComponent_titleIcon, -1)
                startTitle = typedArray.getResourceId(R.styleable.ToolbarComponent_startTitle, -1)
                endTitle = typedArray.getResourceId(R.styleable.ToolbarComponent_endTitle, -1)

            } finally {
                typedArray.recycle()
            }
        }
        bindView()

    }

    fun setStartTextTitle(startTitle: String) {
        if (startTitle.isNotEmpty()) {
            startTitleTv.isVisible = true
            startTitleTv.text = startTitle
        }
    }

    fun setEndTextTitle(endTitle: String) {
        if (endTitle.isNotEmpty()) {
            startTitleTv.isVisible = true
            endTitleTv.text = endTitle
        }
    }

    fun setToolbarTitle(toolbarTitle: String) {
        if (toolbarTitle.isNotEmpty()) {
            toolbarTextView.text = toolbarTitle
        }
    }

    fun setToolbarNavigationIcon(icon : Int) {
       btnBack.setBackgroundResource(icon)
    }

    private fun bindView() {
        if (arrowBackIcon != -1) {
            btnBack.setBackgroundResource(arrowBackIcon)
            btnBack.isVisible = true
        }
        if (toolbarTitle != -1) toolbarTextView.text = context.getString(toolbarTitle)
        if (titleIcon != -1) titleImg.setBackgroundResource(titleIcon)
        if (startTitle != -1) {
            startTitleTv.text = context.getString(startTitle)
            startTitleTv.isVisible = true
        }
        if (endTitle != -1) {
            endTitleTv.text = context.getString(endTitle)
            endTitleTv.isVisible = true
        }


    }

}

