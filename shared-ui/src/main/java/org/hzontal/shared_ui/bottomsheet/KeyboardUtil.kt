package org.hzontal.shared_ui.bottomsheet

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class KeyboardUtil @JvmOverloads constructor(
    contentView: View,
    showKeyboardOnAttach: Boolean = false,
    private val cursorAtEnd: Boolean = false
) {
    init {
        attach(contentView, showKeyboardOnAttach)
    }

    companion object {
        @JvmStatic
        fun hideKeyboard(activity: Activity, view: View?) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }

        @JvmStatic
        fun placeCursorAtEnd(editText: EditText) {
            val text = editText.text ?: return
            editText.setSelection(text.length)
        }

        @JvmStatic
        fun focusAndShowKeyboard(editText: EditText) {
            editText.isFocusable = true
            editText.isFocusableInTouchMode = true
            fun showIme() {
                editText.requestFocus()
                placeCursorAtEnd(editText)
                val imm =
                    editText.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
            editText.post { showIme() }
            editText.postDelayed({ showIme() }, 250)
        }

        private fun findFirstEditText(view: View): EditText? {
            if (view is EditText && view.visibility == View.VISIBLE) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findFirstEditText(view.getChildAt(i))?.let { return it }
                }
            }
            return null
        }
    }

    private fun attach(contentView: View, showKeyboardOnAttach: Boolean) {
        val sheet = generateSequence(contentView) { it.parent as? View }
            .firstOrNull { it.id == com.google.android.material.R.id.design_bottom_sheet }
            ?: contentView
        val editText = findFirstEditText(contentView)

        if (showKeyboardOnAttach && editText != null) {
            focusAndShowKeyboard(editText)
        }

        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val visibleFrame = Rect()
            sheet.getWindowVisibleDisplayFrame(visibleFrame)
            val location = IntArray(2)
            sheet.getLocationOnScreen(location)
            val untranslatedBottom = location[1] - sheet.translationY.toInt() + sheet.height
            val overlap = (untranslatedBottom - visibleFrame.bottom).coerceAtLeast(0)
            val translation = -overlap.toFloat()
            if (sheet.translationY != translation) {
                sheet.translationY = translation
            }
            if (overlap > 0 && editText != null && !editText.hasFocus()) {
                editText.requestFocus()
                if (cursorAtEnd) {
                    placeCursorAtEnd(editText)
                }
            }
        }
        sheet.viewTreeObserver.addOnGlobalLayoutListener(listener)
        sheet.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                v.removeOnAttachStateChangeListener(this)
            }
        })
    }
}
