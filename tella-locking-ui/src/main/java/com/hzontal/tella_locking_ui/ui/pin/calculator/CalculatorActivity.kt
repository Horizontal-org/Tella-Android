package com.hzontal.tella_locking_ui.ui.pin.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils.substring
import android.widget.TextView
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.pin.base.BasePinActivity
import com.hzontal.tella_locking_ui.ui.pin.calculator.Evaluator.eval
import com.hzontal.tella_locking_ui.ui.pin.pinview.CalculatorKeyView
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import timber.log.Timber
import java.lang.String.*
import javax.crypto.spec.PBEKeySpec

private const val TAG = "CalculatorActivity"

class CalculatorActivity : BasePinActivity() {
    private lateinit var calculatorKeyView: CalculatorKeyView
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        initView()
        pinEditText.transformationMethod = null
    }

    private fun initView() {
        calculatorKeyView = findViewById(R.id.pin_lock_view)
        calculatorKeyView.minPinLength = 1
        calculatorKeyView.setPinLockListener(this)

        pinEditText = findViewById(R.id.pin_editText)
        resultText = findViewById(R.id.resultText)
    }

    override fun onSuccessSetPin(pin: String?) {
        TellaKeysUI.getMainKeyStore().load(config.wrapper, PBEKeySpec(pin?.toCharArray()), object : MainKeyStore.IMainKeyLoadCallback {
            override fun onReady(mainKey: MainKey) {
                TellaKeysUI.getMainKeyHolder().set(mainKey);
                onSuccessfulUnlock()
                finish()
            }

            override fun onError(throwable: Throwable) {
                onFailureSetPin(getString(R.string.incorrect_pin_error_msg))
                TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
            }
        })
    }

    @SuppressLint("DefaultLocale")
    override fun onFailureSetPin(error: String) {
        var evaluationString = ""
        try {
            val evaluation = eval(pinEditText.text.toString())
            evaluationString = format("%.2f", evaluation)
            if (substring(
                    evaluationString,
                    evaluationString.length - 3,
                    evaluationString.length
                ) == ".00"
            ) {
                evaluationString = substring(evaluationString, 0, evaluationString.length - 3)
            }
        } catch (e: Exception) {
            evaluationString = "ERROR"
        } finally {
            resultText.setText(evaluationString)
        }
    }

}