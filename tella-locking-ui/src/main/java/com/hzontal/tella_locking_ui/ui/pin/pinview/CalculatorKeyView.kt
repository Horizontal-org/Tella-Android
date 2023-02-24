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
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.databinding.CalculatorKeysViewBinding
import org.hzontal.shared_ui.utils.CalculatorTheme

class CalculatorKeyView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), PinViewListener {
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

    fun initTheme(style: CalculatorThemeStyle, calculatorTheme: CalculatorTheme) {

        when (calculatorTheme) {
            CalculatorTheme.BLUE_SKIN -> {
                binding.btnOperatorsGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundColor(ContextCompat.getColor(context, style.calculatorOperatorsBackgroundColor))
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorOperatorsTextColor))
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(8, 8, 8, 8)
                    button.layoutParams = param
                }

                binding.btnNumbersGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundColor(ContextCompat.getColor(context, style.calculatorNumbersBackgroundColor))
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorNumbersTextColor))
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(8, 8, 8, 8)
                    button.layoutParams = param
                }

                binding.plusMinusBottomBtn.setBackgroundColor(ContextCompat.getColor(context, style.calculatorNumbersBackgroundColor))
                binding.plusMinusBottomBtn.setTextColor(ContextCompat.getColor(context, style.calculatorOperatorsTextColor))
                binding.commaBtn.setBackgroundColor(ContextCompat.getColor(context, style.calculatorNumbersBackgroundColor))
                binding.commaBtn.setTextColor(ContextCompat.getColor(context, style.calculatorOperatorsTextColor))
                binding.okBtn.setBackgroundColor(ContextCompat.getColor(context, style.calculatorOkBtnBackgroundColor))

                binding.plusMinusBtn.text = "()"
                binding.plusMinusBottomBtn.visibility = VISIBLE
                binding.zeroBtnGuideline.setGuidelinePercent(0.25F)

            }

            CalculatorTheme.ORANGE_SKIN -> {

                binding.btnsGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundResource(style.calculatorBackgroundDrawable)
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorNumbersTextColor))
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(8, 8, 8, 8)
                    button.layoutParams = param
                }

                binding.btnOperatorsGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundResource(style.calculatorBackgroundDrawable)
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorOperatorsTextColor))
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(8, 8, 8, 8)
                    button.layoutParams = param
                }
                binding.okBtn.setBackgroundResource(style.calculatorOkBtnTextBackgroundDrawable)
                binding.okBtn.setTextColor(ContextCompat.getColor(context, style.calculatorOkBtnTextColor))
                val param = binding.okBtn.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(8, 8, 8, 8)
                binding.okBtn.layoutParams = param

                binding.plusMinusBottomBtn.text = "0"
                binding.plusMinusBottomBtn.setBackgroundResource(style.calculatorBackgroundDrawable)
                binding.plusMinusBtn.text = "()"
                binding.zeroBtn.text = "."
                binding.deleteBtn.text = "C"
                binding.commaBtn.visibility = GONE
                binding.plusMinusBottomBtn.visibility = VISIBLE
                binding.zeroBtnGuideline.setGuidelinePercent(0.25F)

            }

            CalculatorTheme.YELLOW_SKIN -> {

                binding.btnOperatorsGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundResource(style.calculatorBackgroundDrawable)
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorOperatorsTextColor))
                }

                binding.btnNumbersGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    button.setBackgroundResource(style.calculatorBackgroundDrawable)
                    button.setTextColor(ContextCompat.getColor(context, style.calculatorNumbersTextColor))
                }

                binding.plusMinusBottomBtn.visibility = GONE
                binding.commaBtn.text = "."
                binding.commaBtn.setBackgroundResource(style.calculatorBackgroundDrawable)

                binding.percentBtn.visibility = GONE
                binding.okBtn.setBackgroundResource(style.calculatorOkBtnTextBackgroundDrawable)
                binding.okBtn.setTextColor(ContextCompat.getColor(context, style.calculatorOkBtnTextColor))
            }


            CalculatorTheme.GREEN_SKIN -> {
                binding.btnOperatorsGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(16, 16, 16, 16)
                    button.layoutParams = param
                }
                binding.btnNumbersGroup.referencedIds.forEach {
                    val button = findViewById<TextView>(it)
                    val param = button.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(16, 16, 16, 16)
                    button.layoutParams = param
                }
                val param = binding.commaBtn.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(16, 16, 16, 16)
                binding.commaBtn.layoutParams = param

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