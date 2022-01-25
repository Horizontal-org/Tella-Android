package rs.readahead.washington.mobile.data.entity.uwazi

data class SettingsResponse(
    val __v: Int,
    val _id: String,
    val allowedPublicTemplates: List<String>,
    val analyticsTrackingId: String,
    val contactEmail: String,
    val dateFormat: String,
    val defaultLibraryView: String,
    val favicon: String,
    val filters: List<Any>,
    val home_page: String,
    val languages: List<Language>,
    val links: List<Link>,
    val mailerConfig: String,
    val mapStartingPoint: List<MapStartingPoint>,
    val mapTilerKey: String,
    val matomoConfig: String,
    val `private`: Boolean,
    val publicFormDestination: String,
    val senderEmail: String,
    val site_name: String
)