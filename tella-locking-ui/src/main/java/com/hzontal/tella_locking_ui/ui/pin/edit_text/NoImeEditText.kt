package com.hzontal.tella_locking_ui.ui.pin.edit_text

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager

class NoImeEditText @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatEditText(context, attrs, defStyleAttr) {


    /**
     * This method is called before keyboard appears when text is selected.
     * So just hide the keyboard
     * @return
     */
    override fun onCheckIsTextEditor(): Boolean {
        hideKeyboard()
        return super.onCheckIsTextEditor()
    }

    /**
     * This methdod is called when text selection is changed, so hide keyboard to prevent it to appear
     * @param selStart
     * @param selEnd
     */
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}