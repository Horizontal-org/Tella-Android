package org.hzontal.shared_ui.extensions

import android.content.Context
import android.view.View
import android.widget.TextView

fun TextView.setTextAndVisibility(text: Int, context: Context) {
    if (text != -1) {
        visibility = View.VISIBLE
        setText(context.getString(text))
    }
}