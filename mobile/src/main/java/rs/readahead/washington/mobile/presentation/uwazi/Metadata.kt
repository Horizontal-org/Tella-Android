package rs.readahead.washington.mobile.presentation.uwazi

data class Metadata(
    val age: List<UwaziValue>,
    val comments: List<UwaziValue>,
    val country_of_origin: List<UwaziValue>,
    val date_of_birth: List<UwaziValue>,
    val gender: List<UwaziValue>,
    val internal_code: List<UwaziValue>,
    val multiple_periods: List<Any>,
    val period_1: List<UwaziValue>,
    val picture: List<UwaziValue>,
    val place_where_found_geolocation: List<Any>,
    val profile: List<UwaziValue>,
    val video: List<UwaziValue>,
    val visiting_dates: List<Any>
)