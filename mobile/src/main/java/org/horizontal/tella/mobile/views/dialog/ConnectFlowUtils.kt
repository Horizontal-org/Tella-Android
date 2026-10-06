package org.horizontal.tella.mobile.views.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.Patterns
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.Server

object ConnectFlowUtils {

    fun setUrlFieldError(layout: TextInputLayout, message: CharSequence?) {
        layout.error = null
        if (message.isNullOrBlank()) {
            layout.isHelperTextEnabled = false
            layout.helperText = null
        } else {
            layout.isHelperTextEnabled = true
            layout.setHelperTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(layout.context, R.color.wa_red_error_48)
                )
            )
            layout.helperText = message
        }
    }

    fun validateUrl(field: EditText, layout: TextInputLayout, context: Context, server: Server?): Boolean {
        var url = field.text.toString()
        setUrlFieldError(layout, null)
        if (TextUtils.isEmpty(url)) {
            setUrlFieldError(layout, context.getString(R.string.settings_text_empty_field))
            return false
        } else {
            url = url.trim { it <= ' ' }
            field.setText(url)
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                setUrlFieldError(layout, context.getString(R.string.invalid_url))
                return false
            }
            if (server != null) {
                server.url = url
            }
        }
        return true
    }
}
