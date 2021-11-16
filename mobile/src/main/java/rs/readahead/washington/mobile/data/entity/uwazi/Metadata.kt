package rs.readahead.washington.mobile.data.entity.uwazi

data class Metadata(
    val dateOfIncident: List<DateOfIncident>,
    val description: List<Description>,
    val locationOfIncidentGeolocation: List<LocationOfIncidentGeolocation>,
    val media: List<Media>,
    val typeOfIncident: List<TypeOfIncident>
)