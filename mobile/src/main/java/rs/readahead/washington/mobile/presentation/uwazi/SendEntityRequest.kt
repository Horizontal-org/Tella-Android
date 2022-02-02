package rs.readahead.washington.mobile.presentation.uwazi

data class SendEntityRequest(
    val attachments: List<Attachment>?,
    val metadata: Metadata,
    val template: String,
    val title: String,
    val type: String
)