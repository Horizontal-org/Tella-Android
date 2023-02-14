package com.hzontal.tella_locking_ui.ui.pin.pinview
/*
    Simplified PinLockView
*/

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.hzontal.tella_locking_ui.R

class CalculatorKeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),PinViewListener{
    var minPinLength = 1
    private var mPinLockListener: PinLockListener? = null
    private lateinit var mGroupButtons : Group
    private lateinit var mGroupOperatorsButtons : Group
    private lateinit var mOnKeyBoardClickListener : OnKeyBoardClickListener
    private lateinit var mResultListener : ResultListener
    private lateinit var mOkButton : TextView
    private lateinit var deleteButton : TextView
    private var imageTintColor : Int = 0
    private lateinit var imageBackgroundImg : Drawable

    init {
        LayoutInflater.from(context).inflate(R.layout.calculator_keys_view, this, true)
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.AwesomeCustomView)
            val imageTintColorResource = typedArray.getResourceId(R.styleable.AwesomeCustomView_awesomeImageTintColor,  android.R.color.white)
            val imagerResource = typedArray.getResourceId(R.styleable.AwesomeCustomView_awesomeImage,-1)
             imageTintColor = ContextCompat.getColor(context, imageTintColorResource)
            if(imagerResource != -1)
             imageBackgroundImg  = ContextCompat.getDrawable(context, imagerResource)!!
            typedArray.recycle()
        }
        initView()
    }

    private fun initView() {
        mGroupButtons = findViewById(R.id.btnsGroup)
        mGroupOperatorsButtons = findViewById(R.id.btnOperatorsGroup)
        mOkButton = findViewById(R.id.okBtn)
        deleteButton = findViewById(R.id.deleteBtn)
        deleteButton.setTextColor(imageTintColor)
        deleteButton.background = imageBackgroundImg
    }

    fun setListenerers(pinLockListener: PinLockListener?, resultListener: ResultListener?) {
        mPinLockListener = pinLockListener
        if (resultListener != null) {
            mResultListener = resultListener
        }
        mOnKeyBoardClickListener = OnKeyBoardClickListener(minPinLength,pinLockListener,this)
        initListeners()
    }

    private fun initListeners () {
        mGroupButtons.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.setOnClickListener(mOnKeyBoardClickListener)
        }
        mGroupOperatorsButtons.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.setBackgroundColor(imageTintColor)

        }
        deleteButton.setOnClickListener{
            mResultListener.onClearResult()
            mOnKeyBoardClickListener.onClearClicked()
        }
    }

    override fun onHiLightView(pin: String) {
    }
}