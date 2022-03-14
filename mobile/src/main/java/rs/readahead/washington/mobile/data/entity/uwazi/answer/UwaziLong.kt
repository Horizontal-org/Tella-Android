package rs.readahead.washington.mobile.data.entity.uwazi.answer

class UwaziLong(
    var value: Long
) : IUwaziAnswer {
    override fun setValue(o: Any?) {
        if (o == null) {
            throw NullPointerException("IUwaziAnswer can't be null")
        }
        value = (o as Long).toLong()
    }

    override fun getValue(): Any {
        return value
    }

    override fun getDisplayText(): String {
        return value.toString()
    }

    fun UwaziLong(n: Long) {
        value = n
    }
}