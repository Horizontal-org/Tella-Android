package org.horizontal.tella.mobile.views.fragment.peertopeer.common

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.widget.EditText
import org.horizontal.tella.mobile.domain.peertopeer.IpAddressInputMask

class IpAddressMaskEditText private constructor(
    private val editText: EditText,
) {
    private var value = ""
    private var isInternalChange = false

    val canonicalIp: String?
        get() = IpAddressInputMask.toCanonicalIp(value)

    val isComplete: Boolean
        get() = IpAddressInputMask.isValidCompleteIp(value)

    fun attach(onChanged: (() -> Unit)? = null) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.keyListener = DigitsKeyListener.getInstance("0123456789.")
        editText.filters = arrayOf(InputFilter.LengthFilter(IpAddressInputMask.MAX_LENGTH))

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, before: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isInternalChange) return

                when {
                    count > 0 -> handleAddition(s, start, count)
                    before > 0 -> handleDeletion(before)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                syncDisplay(s, onChanged)
            }
        })
    }

    private fun handleAddition(s: CharSequence?, start: Int, count: Int) {
        val inserted = s?.subSequence(start, start + count)?.toString().orEmpty()
        val next = IpAddressInputMask.applyInput(value, inserted) ?: return
        if (IpAddressInputMask.isPartialValid(next)) {
            value = next
        }
    }

    private fun handleDeletion(deletedCount: Int) {
        repeat(deletedCount.coerceAtMost(value.length)) {
            value = value.dropLast(1)
        }
    }

    private fun syncDisplay(editable: Editable?, onChanged: (() -> Unit)?) {
        if (editable == null) return
        if (editable.toString() == value) {
            editText.post { onChanged?.invoke() }
            return
        }

        isInternalChange = true
        val savedFilters = editText.filters
        editText.filters = emptyArray()
        editable.replace(0, editable.length, value)
        val selection = value.length.coerceIn(0, editable.length)
        if (editText.selectionStart != selection) {
            editText.setSelection(selection)
        }
        editText.filters = savedFilters
        isInternalChange = false
        editText.post { onChanged?.invoke() }
    }

    companion object {
        fun attach(editText: EditText, onChanged: (() -> Unit)? = null): IpAddressMaskEditText {
            return IpAddressMaskEditText(editText).also { it.attach(onChanged) }
        }
    }
}
