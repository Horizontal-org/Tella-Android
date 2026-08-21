package org.horizontal.tella.mobile.data.entity.uwazi.answer


data class UwaziString(
    var value: String
): IUwaziAnswer {

    override fun setValue(o: Any?) {
        if (o == null) {
            throw NullPointerException("IUwaziAnswer can't be null")
        }
        value = o as String
    }

    override fun getValue(): Any {
        return value
    }

    override fun getDisplayText(): String {
        return value
    }

    fun UwaziString(s: String) {
        setValue(s)
    }
}
