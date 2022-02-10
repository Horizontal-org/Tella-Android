package rs.readahead.washington.mobile.data.entity.uwazi.answer

import com.google.gson.Gson

class UwaziDateRange(
    var from: Int,
    var to: Int
) : IUwaziAnswer {

    override fun setValue(o: Any?) {
        val sRange: String = o as String
        val range = Gson().fromJson(sRange, UwaziDateRange::class.java)
        this.from = range.from
        this.to = range.to
    }

    override fun getValue(): Any {
        return UwaziDateRange(this.from, this.to)
    }

    override fun getDisplayText(): String {
        return Gson().toJson(this)
    }
}