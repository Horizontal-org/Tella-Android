package com.hzontal.tella_locking_ui.ui.pin.pinview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.hzontal.tella_locking_ui.R

/**
 * Represents a numeric lock view which can used to taken numbers as input.
 * The length of the input can be customized using [PinLockView.setMinPinLength] (int)}, the default value being 6
 *
 *
 * It can also be used as dial pad for taking number inputs.
 */
class PinLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),PinViewListener{
    var minPinLength = 0
    private var mHorizontalSpacing = 0
    private var mVerticalSpacing = 0
    private var mTextColor = 0
    private var mDeleteButtonPressedColor = 0
    private var mOffTextColor = 0
    private var mTextSize = 0
    private var mButtonSize = 0
    private var mDeleteButtonSize = 0
    private var mButtonBackgroundDrawable: Drawable? = null
    private var mDeleteButtonDrawable: Drawable? = null
    private var mShowDeleteButton = false
    private var mPinLockListener: PinLockListener? = null
    private var mCustomizationOptionsBundle: CustomizationOptionsBundle? = null
    private lateinit var mGroupButtons : Group
    private lateinit var mOnKeyBoardClickListener : OnKeyBoardClickListener
    private lateinit var mOkButton : TextView
    companion object {
        const val DEFAULT_PIN_LENGTH = 6
        private val DEFAULT_KEY_SET = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this, true)
        initView()
        init(attrs, defStyleAttr)
    }

    private fun init(attributeSet: AttributeSet?, defStyle: Int) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.PinLockView)
        try {
            minPinLength = typedArray.getInt(R.styleable.PinLockView_pinLength, DEFAULT_PIN_LENGTH)
            mHorizontalSpacing = typedArray.getDimension(R.styleable.PinLockView_keypadHorizontalSpacing, ResourceUtils.getDimensionInPx(context, R.dimen.default_horizontal_spacing)).toInt()
            mVerticalSpacing = typedArray.getDimension(R.styleable.PinLockView_keypadVerticalSpacing, ResourceUtils.getDimensionInPx(context, R.dimen.default_vertical_spacing)).toInt()
            mTextColor = typedArray.getColor(R.styleable.PinLockView_keypadTextColor, ResourceUtils.getColor(context, R.color.wa_white))
            mOffTextColor = typedArray.getColor(R.styleable.PinLockView_keypadOffTextColor, ResourceUtils.getColor(context, R.color.wa_white_40))
            mTextSize = typedArray.getDimension(R.styleable.PinLockView_keypadTextSize, ResourceUtils.getDimensionInPx(context, R.dimen.default_text_size)).toInt()
            mButtonSize = typedArray.getDimension(R.styleable.PinLockView_keypadButtonSize, ResourceUtils.getDimensionInPx(context, R.dimen.default_button_size)).toInt()
            mDeleteButtonSize = typedArray.getDimension(R.styleable.PinLockView_keypadDeleteButtonSize, ResourceUtils.getDimensionInPx(context, R.dimen.default_delete_button_size)).toInt()
            mButtonBackgroundDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadButtonBackgroundDrawable)
            mDeleteButtonDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadDeleteButtonDrawable)
            mShowDeleteButton = typedArray.getBoolean(R.styleable.PinLockView_keypadShowDeleteButton, true)
            mDeleteButtonPressedColor = typedArray.getColor(R.styleable.PinLockView_keypadDeleteButtonPressedColor, ResourceUtils.getColor(context, R.color.wa_orange))
        } finally {
            typedArray.recycle()
        }
        mCustomizationOptionsBundle = CustomizationOptionsBundle()
        mCustomizationOptionsBundle!!.textColor = mTextColor
        mCustomizationOptionsBundle!!.seOffTextColor(mOffTextColor)
        mCustomizationOptionsBundle!!.textSize = mTextSize
        mCustomizationOptionsBundle!!.buttonSize = mButtonSize
        mCustomizationOptionsBundle!!.buttonBackgroundDrawable = mButtonBackgroundDrawable
        mCustomizationOptionsBundle!!.deleteButtonDrawable = mDeleteButtonDrawable
        mCustomizationOptionsBundle!!.deleteButtonSize = mDeleteButtonSize
        mCustomizationOptionsBundle!!.isShowDeleteButton = mShowDeleteButton
        mCustomizationOptionsBundle!!.deleteButtonPressesColor = mDeleteButtonPressedColor
        initView()
    }

    private fun initView() {
        mGroupButtons = findViewById(R.id.btnsGroup)
        mOkButton = findViewById(R.id.okBtn)
    }

    /**
     * Sets a [PinLockListener] to the to listen to pin update events
     *
     * @param pinLockListener the listener
     */
    fun setPinLockListener(pinLockListener: PinLockListener?) {
        mPinLockListener = pinLockListener
        mOnKeyBoardClickListener = OnKeyBoardClickListener(minPinLength,pinLockListener,this)
        initListeners()
    }


    /**
     * init buttons listener
     */

    private fun initListeners () {
        mGroupButtons.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.setOnClickListener(mOnKeyBoardClickListener)
        }
       val deleteButton = findViewById<ImageView>(R.id.deleteBtn)
        deleteButton.setOnClickListener(mOnKeyBoardClickListener)
    }


    override fun onHiLightView(pin: String) {
        if (pin.length >= DEFAULT_PIN_LENGTH) {
            mOkButton.setTextColor(mCustomizationOptionsBundle!!.textColor)
        } else {
            mOkButton.setTextColor(mCustomizationOptionsBundle!!.offTextColor)
        }
    }


}