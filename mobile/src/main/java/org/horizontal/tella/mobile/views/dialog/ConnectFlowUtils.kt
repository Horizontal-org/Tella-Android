package org.horizontal.tella.mobile.views.dialog

import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.Server

object ConnectFlowUtils {

     fun validateUrl(field: EditText, layout: TextInputLayout, context: Context, server : Server?) : Boolean {

        var url = field.text.toString()
        layout.error = null
        if (TextUtils.isEmpty(url)) {
            layout.error = context.getString(R.string.settings_text_empty_field)
            return false
        } else {
            url = url.trim { it <= ' ' }
            field.setText(url)
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                layout.error = context.getString(R.string.settings_docu_error_not_valid_URL)
               return false
            }
            if (server!=null){
                server.url = url
            }
        }
        return true
    }
}