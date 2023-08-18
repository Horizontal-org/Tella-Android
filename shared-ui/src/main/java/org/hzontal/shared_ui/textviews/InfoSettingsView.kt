package org.hzontal.shared_ui.textviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import org.hzontal.shared_ui.R

class InfoSettingsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val onClickListener: (() -> Unit) = {}

    @StringRes
    private var titleText: Int = -1

    @StringRes
    private var labelText: Int = -1

    @StringRes
    private var infoText: Int = -1

    private var isBottomLineVisible = false

    private lateinit var titleTextview: TextView
    private lateinit var infoTextview: TextView
    private lateinit var labelTextview: TextView
    private lateinit var bottomLineView: View

    init {
        LayoutInflater.from(context).inflate(R.layout.settings_info_view, this, true)
        initView()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        titleTextview = findViewById(R.id.title_textview)
        infoTextview = findViewById(R.id.info_textview)
        labelTextview = findViewById(R.id.label_textview)
        bottomLineView = findViewById(R.id.bottom_line_view)
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.InfoSettingsView, defStyleAttr, defStyleAttr
            )

            try {

                titleText =
                    typedArray.getResourceId(R.styleable.InfoSettingsView_settingsTitleText, -1)
                labelText =
                    typedArray.getResourceId(R.styleable.InfoSettingsView_settingsLabelText, -1)
                infoText =
                    typedArray.getResourceId(R.styleable.InfoSettingsView_settingsInfoText, -1)
                isBottomLineVisible =
                    typedArray.getBoolean(R.styleable.InfoSettingsView_isBottomLineVisible, true)

            } finally {
                typedArray.recycle()
            }
        }
        setTextAndVisibility(titleText, titleTextview)
        setTextAndVisibility(infoText, infoTextview)
        setTextAndVisibility(labelText, labelTextview)
        isBottomLineVisible(isBottomLineVisible)


        labelTextview.setOnClickListener {
            onClickListener.invoke()
        }
    }

    fun isBottomLineVisible(isBottomLineVisible: Boolean) {
        bottomLineView.isVisible = isBottomLineVisible
    }

    fun setLabelColor(color : Int){
        labelTextview.setTextColor(ContextCompat.getColor(context,color))
    }

    private fun setTextAndVisibility(text: Int, textView: TextView) {
        if (text != -1) {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(text)
        }
    }


    fun setTitleText(text: CharSequence) {
        titleTextview.text = text
    }

    fun setInfoText(text: CharSequence) {
        infoTextview.isVisible = text.isNotEmpty()
        infoTextview.text = text
    }

    fun setLabelText(text: CharSequence) {
        labelTextview.text = text
    }


}
