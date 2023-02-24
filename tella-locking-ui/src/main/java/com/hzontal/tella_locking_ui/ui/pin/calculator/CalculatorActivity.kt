package com.hzontal.tella_locking_ui.ui.pin.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.marginEnd
import androidx.core.widget.TextViewCompat
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.databinding.ActivityCalculatorBinding
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResultListener
import org.hzontal.shared_ui.utils.CALCULATOR_THEME
import org.hzontal.shared_ui.utils.CalculatorTheme
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import javax.crypto.spec.PBEKeySpec

private const val TAG = "CalculatorActivity"

class CalculatorActivity : BasePinActivity(), ResultListener {
    private var calculatorTheme: String? = null
    private lateinit var binding: ActivityCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        calculatorTheme = intent.extras?.getString(CALCULATOR_THEME)
        setContentView(binding.root)
        initView()
        setTheme()
        pinEditText.transformationMethod = null

    }

    private fun setTheme() {
        when (calculatorTheme) {
            CalculatorTheme.BLUE_SKIN.name -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.wa_black))
                binding.pinLockView.initTheme(ThemeManager.getTheme(CalculatorTheme.BLUE_SKIN), CalculatorTheme.BLUE_SKIN)
                TextViewCompat.setTextAppearance(pinEditText, R.style.Calculator_Edit_Text_Blue_Skin)
                TextViewCompat.setTextAppearance(binding.resultText, R.style.Calculator_Result_Text_Blue_Skin)
            }
            CalculatorTheme.ORANGE_SKIN.name -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.wa_white))
                binding.pinLockView.initTheme(ThemeManager.getTheme(CalculatorTheme.ORANGE_SKIN), CalculatorTheme.ORANGE_SKIN)
                TextViewCompat.setTextAppearance(pinEditText, R.style.Calculator_Edit_Text_Orange_Skin)
                TextViewCompat.setTextAppearance(binding.resultText, R.style.Calculator_Result_Text_Orange_Skin)
            }
            CalculatorTheme.GREEN_SKIN.name -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.wa_white))
                val param = binding.pinLockView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(10,10,10,10)
                binding.pinLockView.layoutParams = param
                binding.pinLockView.initTheme(ThemeManager.getTheme(CalculatorTheme.GREEN_SKIN), CalculatorTheme.GREEN_SKIN)

            }
            CalculatorTheme.YELLOW_SKIN.name -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.wa_white))
                binding.pinLockView.initTheme(ThemeManager.getTheme(CalculatorTheme.YELLOW_SKIN), CalculatorTheme.YELLOW_SKIN)
                TextViewCompat.setTextAppearance(pinEditText, R.style.Calculator_Edit_Text_Orange_Skin)
                TextViewCompat.setTextAppearance(binding.resultText, R.style.Calculator_Result_Text_Orange_Skin)

            }

        }
    }


    private fun initView() {

        binding.pinLockView.minPinLength = 1
        binding.pinLockView.setListenerers(this, this)
        onClearResult()
        pinEditText = binding.pinEditText

    }

    override fun onSuccessSetPin(pin: String?) {
        TellaKeysUI.getMainKeyStore().load(
                config.wrapper,
                PBEKeySpec(pin?.toCharArray()),
                object : MainKeyStore.IMainKeyLoadCallback {
                    override fun onReady(mainKey: MainKey) {
                        TellaKeysUI.getMainKeyHolder().set(mainKey);
                        onSuccessfulUnlock()
                        finish()
                    }

                    override fun onError(throwable: Throwable) {
                        onFailureSetPin(getString(R.string.LockPinConfirm_Message_Error_IncorrectPin))
                        TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
                    }
                })
    }

    @SuppressLint("DefaultLocale")
    override fun onFailureSetPin(error: String) {
        var evaluationString = ""
        try {
            evaluationString = Evaluator.evaluateResult(pinEditText.text.toString())
        } catch (e: Exception) {
            evaluationString = "ERROR"
        } finally {
            binding.resultText.text = evaluationString
        }
    }

    override fun onClearResult() {
        binding.resultText.text = "0"
    }
}