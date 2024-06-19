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
    private lateinit var bigStartTitleTv: TextView
    private lateinit var endTitleTv: TextView
    private lateinit var rightImg : AppCompatImageButton
    private lateinit var leftImg : AppCompatImageButton
    private lateinit var btnRightOfLeftImage: AppCompatImageButton

    @DrawableRes
    private var arrowBackIcon: Int = -1
    @DrawableRes
    private var titleIcon: Int = -1
    private var rightIcon : Int = -1
    private var leftIcon : Int = -1
    @StringRes
    var rightIconContentDescription : Int = -1
    @StringRes
    var leftIconContentDescription : Int = -1
    @StringRes
    var arrowBackIconContentDescription : Int = -1
    @DrawableRes
    private var rightOfLeftIcon: Int = -1
    @StringRes
    private var rightOfLeftIconContentDescription: Int = -1

    var toolbarTitle: Int = -1
    var startTitle: Int = -1
    var bigStartTitle: Int = -1
    var endTitle: Int = -1

    var backClickListener: (() -> Unit)? = null
    var onRightClickListener: (() -> Unit)? = null
    var onLeftClickListener: (() -> Unit)? = null
    var onRightOfLeftClickListener: (() -> Unit)? = null


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
        endTitleTv = findViewById(R.id.endTitleTv)
        rightImg = findViewById(R.id.right_img)
        leftImg = findViewById(R.id.left_img)
        bigStartTitleTv = findViewById(R.id.bigStartTitleTv)
        btnRightOfLeftImage = findViewById(R.id.btn_right_of_left_image)
    }

    private fun initListener() {
        btnBack.setOnClickListener {
            backClickListener?.invoke()
        }
        rightImg.setOnClickListener {
            onRightClickListener?.invoke()
        }
        leftImg.setOnClickListener {
            onLeftClickListener?.invoke()
        }
        btnRightOfLeftImage.setOnClickListener {
            onRightOfLeftClickListener?.invoke()
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
                arrowBackIconContentDescription = typedArray.getResourceId(R.styleable.ToolbarComponent_arrowBackIconContentDescription, -1)
                titleIcon = typedArray.getResourceId(R.styleable.ToolbarComponent_titleIcon, -1)
                startTitle = typedArray.getResourceId(R.styleable.ToolbarComponent_startTitle, -1)
                bigStartTitle = typedArray.getResourceId(R.styleable.ToolbarComponent_bigStartTitle, -1)
                endTitle = typedArray.getResourceId(R.styleable.ToolbarComponent_endTitle, -1)
                rightIcon = typedArray.getResourceId(R.styleable.ToolbarComponent_rightIcon, -1)
                rightIconContentDescription = typedArray.getResourceId(R.styleable.ToolbarComponent_rightIconContentDescription, -1)
                leftIcon = typedArray.getResourceId(R.styleable.ToolbarComponent_leftIcon, -1)
                leftIconContentDescription = typedArray.getResourceId(R.styleable.ToolbarComponent_leftIconContentDescription, -1)
                rightOfLeftIcon = typedArray.getResourceId(R.styleable.ToolbarComponent_rightOfLeftIcon, -1)
                rightOfLeftIconContentDescription = typedArray.getResourceId(R.styleable.ToolbarComponent_rightOfLeftIconContentDescription, -1)
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

    fun setBackIcon(icon: Int){
        if (icon != -1) {
            btnBack.setBackgroundResource(icon)
            btnBack.isVisible = true
        }
    }

    fun setRightIcon(icon: Int){
        if (icon != -1){
            rightImg.setBackgroundResource(icon)
            rightImg.isVisible = true
        }else{
            rightImg.isVisible = false
        }
    }

    fun setLeftIcon(icon: Int){
        if (icon != -1){
            leftImg.setBackgroundResource(icon)
            leftImg.isVisible = true
        }else{
            leftImg.isVisible = false
        }
    }

    fun setRightOfLeftIcon(icon: Int){
        if (icon != -1){
            btnRightOfLeftImage.setBackgroundResource(icon)
            btnRightOfLeftImage.isVisible = true
        }else{
            btnRightOfLeftImage.isVisible = false
        }
    }

    private fun bindView() {
        if (arrowBackIcon != -1) {
            btnBack.setBackgroundResource(arrowBackIcon)
            btnBack.isVisible = true
        }
        if (arrowBackIconContentDescription != -1) {
            btnBack.contentDescription = context.getString(arrowBackIconContentDescription)
        }
        if (toolbarTitle != -1){
            toolbarTextView.text = context.getString(toolbarTitle)
            toolbarTextView.isVisible = true
        }
        if (titleIcon != -1) titleImg.setBackgroundResource(titleIcon)
        if (startTitle != -1) {
            startTitleTv.text = context.getString(startTitle)
            startTitleTv.isVisible = true
        }
        if (bigStartTitle != -1) {
            bigStartTitleTv.text = context.getString(bigStartTitle)
            bigStartTitleTv.isVisible = true
        }
        if (endTitle != -1) {
            endTitleTv.text = context.getString(endTitle)
            endTitleTv.isVisible = true
        }
        if (rightIcon != -1){
            rightImg.setBackgroundResource(rightIcon)
            rightImg.isVisible = true
        }
        if (leftIcon != -1){
            leftImg.setBackgroundResource(leftIcon)
            leftImg.isVisible = true
        }
        if (rightIconContentDescription != -1){
            rightImg.contentDescription = context.getString(rightIconContentDescription)
        }
        if (leftIconContentDescription != -1){
            leftImg.contentDescription = context.getString(leftIconContentDescription)
        }

        if (rightOfLeftIcon != -1) {
            btnRightOfLeftImage.setBackgroundResource(rightOfLeftIcon)
            btnRightOfLeftImage.isVisible = true
        }
        if (rightOfLeftIconContentDescription != -1) {
            btnRightOfLeftImage.contentDescription = context.getString(rightOfLeftIconContentDescription)
        }


    }

}

