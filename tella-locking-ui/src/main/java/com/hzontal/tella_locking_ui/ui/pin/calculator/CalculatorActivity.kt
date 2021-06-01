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
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResultListener
import org.hzontal.tella.keys.MainKeyStore
import org.hzontal.tella.keys.key.MainKey
import java.lang.String.format
import java.text.DecimalFormat
import javax.crypto.spec.PBEKeySpec

private const val TAG = "CalculatorActivity"

class CalculatorActivity : BasePinActivity(), ResultListener {
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
        calculatorKeyView.setListenerers(this, this)

        pinEditText = findViewById(R.id.pin_editText)
        resultText = findViewById(R.id.resultText)
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
                    onFailureSetPin(getString(R.string.incorrect_pin_error_msg))
                    TellaKeysUI.getCredentialsCallback().onUnSuccessfulUnlock(TAG, throwable)
                }
            })
    }

    @SuppressLint("DefaultLocale")
    override fun onFailureSetPin(error: String) {
        var evaluationString = ""
        try {
            evaluationString = evaluateResult(pinEditText.text.toString())
        } catch (e: Exception) {
            evaluationString = "ERROR"
        } finally {
            resultText.setText(evaluationString)
        }
    }

    override fun onClearResult() {
        resultText.setText("")
    }

    private fun evaluateResult(input: String): String {
        var entry = input
        entry = entry.replace('x', '*')
        entry = entry.replace('÷', '/')
        entry = entry.replace(',', '.')
        entry = entry.replace(" ", "")
        val evaluation = eval(entry)
        var evaluationString = format("%s", evaluation)
        if (substring(
                evaluationString,
                evaluationString.length - 2,
                evaluationString.length
            ) == ".0"
        ) {
            evaluationString = substring(evaluationString, 0, evaluationString.length - 2)
        }
        if (evaluationString.length > 10) {
            val sf = DecimalFormat("0.#####E0")
            evaluationString = sf.format(evaluation)
        }
        return evaluationString
    }
}