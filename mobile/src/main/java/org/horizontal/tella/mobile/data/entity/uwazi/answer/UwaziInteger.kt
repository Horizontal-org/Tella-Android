package org.horizontal.tella.mobile.data.entity.uwazi.answer

data class UwaziInteger (
    var value: Int
): IUwaziAnswer {
    override fun setValue(o: Any?) {
        if (o == null) {
            throw NullPointerException("IUwaziAnswer can't be null")
        }
        value = (o as Int).toInt()
    }

    override fun getValue(): Any {
        return Integer.valueOf(value)
    }

    override fun getDisplayText(): String {
        return value.toString()
    }

    fun UwaziInteger(n: Int) {
        value = n
    }
}
