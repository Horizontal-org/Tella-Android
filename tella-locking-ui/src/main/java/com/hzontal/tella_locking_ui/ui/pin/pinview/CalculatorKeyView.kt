package com.hzontal.tella_locking_ui.ui.pin.pinview
/*
    Simplified PinLockView
*/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import com.hzontal.tella_locking_ui.databinding.CalculatorKeysViewBinding
import org.hzontal.shared_ui.utils.CalculatorTheme

class CalculatorKeyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr), PinViewListener {
    var minPinLength = 1
    private var mPinLockListener: PinLockListener? = null
    private lateinit var mOnKeyBoardClickListener: OnKeyBoardClickListener
    private lateinit var mResultListener: ResultListener

    private val binding: CalculatorKeysViewBinding = CalculatorKeysViewBinding.inflate(LayoutInflater.from(context), this, true)

    fun setListenerers(pinLockListener: PinLockListener?, resultListener: ResultListener?) {
        mPinLockListener = pinLockListener
        if (resultListener != null) {
            mResultListener = resultListener
        }
        mOnKeyBoardClickListener = OnKeyBoardClickListener(minPinLength, pinLockListener, this)
        initListeners()
    }

    fun initTheme(style: CalculatorThemeStyle, calculatorTheme: CalculatorTheme) = when (calculatorTheme) {
        CalculatorTheme.BLUE_SKIN -> {
            setButtonsGroupStyleWithBackgroundColor(binding.btnOperatorsGroup, style.calculatorOperatorsBackgroundColor, style.calculatorOperatorsTextColor, 8)
            setButtonsGroupStyleWithBackgroundColor(binding.btnNumbersGroup, style.calculatorNumbersBackgroundColor, style.calculatorNumbersTextColor, 8)
            setButtonStyleWithBackgroundColor(binding.plusMinusBottomBtn, style.calculatorNumbersBackgroundColor, style.calculatorOperatorsTextColor, 8)
            setButtonStyleWithBackgroundColor(binding.commaBtn, style.calculatorNumbersBackgroundColor, style.calculatorOperatorsTextColor, 8)
            setButtonStyleWithBackgroundColor(binding.okBtn, style.calculatorOkBtnBackgroundColor, style.calculatorOperatorsTextColor, 8)
            binding.plusMinusBtn.text = "()"
            binding.plusMinusBottomBtn.visibility = VISIBLE
            binding.zeroBtnGuideline.setGuidelinePercent(0.25F)
        }

        CalculatorTheme.ORANGE_SKIN -> {
            setButtonsGroupStyleWithBackgroundResource(binding.btnNumbersGroup, style.calculatorBackgroundDrawable, style.calculatorNumbersTextColor, 0)
            setButtonsGroupStyleWithBackgroundResource(binding.btnOperatorsGroup, style.calculatorBackgroundDrawable, style.calculatorOperatorsTextColor, 0)
            binding.okBtn.apply {
                setBackgroundResource(style.calculatorOkBtnTextBackgroundDrawable)
                setTextColor(ContextCompat.getColor(context, style.calculatorOkBtnTextColor))
            }
            binding.plusMinusBottomBtn.apply {
                setBackgroundResource(style.calculatorBackgroundDrawable)
                text = "0"
                visibility = VISIBLE
            }
            binding.plusMinusBtn.text = "()"
            binding.zeroBtn.text = "."
            binding.deleteBtn.text = "C"
            binding.commaBtn.setBackgroundResource(style.calculatorBackgroundDrawable)
            binding.zeroBtnGuideline.setGuidelinePercent(0.25F)

        }

        CalculatorTheme.YELLOW_SKIN -> {
            setButtonsGroupStyleWithBackgroundResource(binding.btnOperatorsGroup, style.calculatorBackgroundDrawable, style.calculatorOperatorsTextColor, 0)
            setButtonsGroupStyleWithBackgroundResource(binding.btnNumbersGroup, style.calculatorBackgroundDrawable, style.calculatorNumbersTextColor, 0)
            binding.plusMinusBottomBtn.visibility = GONE
            binding.commaBtn.apply {
                text = "."
                setBackgroundResource(style.calculatorBackgroundDrawable)
            }
            binding.okBtn.apply {
                setBackgroundResource(style.calculatorOkBtnTextBackgroundDrawable)
                setTextColor(ContextCompat.getColor(context, style.calculatorOkBtnTextColor))
            }
            binding.percentBtn.visibility = GONE
        }
        CalculatorTheme.GREEN_SKIN -> {
            setButtonsGroupMargin(binding.btnOperatorsGroup, 16)
            setButtonsGroupMargin(binding.btnNumbersGroup, 16)
            setTextViewMargin(binding.commaBtn, 16)
            setTextViewMargin(binding.okBtn, 16)
        }
    }

    private fun setTextViewMargin(button: TextView, margin: Int) {
        button.apply {
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(margin)
            layoutParams = params
        }
    }

    private fun setButtonsGroupMargin(group: Group, margin: Int) {
        group.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.apply {
                val params = layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(margin)
                layoutParams = params
            }
        }
    }

    private fun setButtonsGroupStyleWithBackgroundColor(group: Group, backgroundColor: Int, textColor: Int, margin: Int) {
        group.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.apply {
                setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
                setTextColor(ContextCompat.getColor(context, textColor))
                val params = layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(margin)
                layoutParams = params
            }
        }
    }

    private fun setButtonStyleWithBackgroundColor(button: TextView, backgroundColor: Int, textColor: Int, margin: Int) {
        button.apply {
            setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
            setTextColor(ContextCompat.getColor(context, textColor))
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(margin)
            layoutParams = params
        }
    }


    private fun setButtonsGroupStyleWithBackgroundResource(group: Group, backgroundResource: Int, textColor: Int, margin: Int) {
        group.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.apply {
                setBackgroundResource(backgroundResource)
                setTextColor(ContextCompat.getColor(context, textColor))
                val params = layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(margin)
                layoutParams = params
            }
        }
    }

    private fun initListeners() {
        binding.btnsGroup.referencedIds.forEach {
            val button = findViewById<TextView>(it)
            button.setOnClickListener(mOnKeyBoardClickListener)
        }

        binding.deleteBtn.setOnClickListener {
            mResultListener.onClearResult()
            mOnKeyBoardClickListener.onClearClicked()
        }
    }

    override fun onHiLightView(pin: String) {
    }
}