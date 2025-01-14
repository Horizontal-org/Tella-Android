package org.horizontal.tella.mobile.data.entity.uwazi.answer

import com.google.gson.Gson

data class UwaziLocation(
    var lat: Double,
    var lon: Double,
    var label: String
): IUwaziAnswer {
    override fun setValue(o: Any?) {
        val sLoc : String = o as String
        val uloc = Gson().fromJson(sLoc, UwaziLocation::class.java)
        this.label = uloc.label
        this.lon = uloc.lon
        this.lat = uloc.lat
    }

    override fun getValue(): Any {
        return UwaziLocation(this.lat,this.lon,this.label);
    }

    override fun getDisplayText(): String {
        return Gson().toJson(this)
    }

    fun UwaziLocation (lat: Double, lon: Double, label: String){
        this.lat = lat
        this.lon = lon
        this.label = label
    }
}