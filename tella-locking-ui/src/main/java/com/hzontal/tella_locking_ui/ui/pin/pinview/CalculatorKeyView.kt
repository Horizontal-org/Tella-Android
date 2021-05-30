package com.hzontal.tella_locking_ui.ui.pin.pinview
/*
    Simplified PinLockView
*/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.hzontal.tella_locking_ui.R

class CalculatorKeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),PinViewListener{
    var minPinLength = 1
    private var mPinLockListener: PinLockListener? = null
    private lateinit var mGroupButtons : Group
    private lateinit var mOnKeyBoardClickListener : OnKeyBoardClickListener
    private lateinit var mOkButton : TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.calculator_keys_view, this, true)
        initView()
    }

    private fun initView() {
        mGroupButtons = findViewById(R.id.btnsGroup)
        mOkButton = findViewById(R.id.okBtn)
    }

    fun setPinLockListener(pinLockListener: PinLockListener?) {
        mPinLockListener = pinLockListener
        mOnKeyBoardClickListener = OnKeyBoardClickListener(minPinLength,pinLockListener,this)
        initListeners()
    }

    private fun initListeners () {
        mGroupButtons.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.setOnClickListener(mOnKeyBoardClickListener)
        }
        val deleteButton = findViewById<TextView>(R.id.deleteBtn)
        deleteButton.setOnClickListener(mOnKeyBoardClickListener)
    }

    override fun onHiLightView(pin: String) {
    }
}