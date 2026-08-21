package org.hzontal.shared_ui.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

// Extension function to observe text changes in EditText
fun EditText.onTextChanged(action: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            action(s.toString())
        }
    })
}
