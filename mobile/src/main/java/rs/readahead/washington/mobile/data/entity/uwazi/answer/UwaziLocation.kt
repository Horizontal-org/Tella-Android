package rs.readahead.washington.mobile.data.entity.uwazi.answer

data class UwaziLocation(
    var lat: Double,
    var lon: Double,
    var label: String
): IUwaziAnswer {
    override fun setValue(o: Any?) {
        TODO("Not yet implemented")
    }

    override fun getValue(): Any {
        TODO("Not yet implemented")
    }

    override fun getDisplayText(): String {
        TODO("Not yet implemented")
    }

    fun UwaziLocation (lat: Double, lon: Double, label: String){
        this.lat = lat
        this.lon = lon
        this.label = label
    }
}