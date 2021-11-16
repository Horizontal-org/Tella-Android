package rs.readahead.washington.mobile.data.entity.uwazi

data class UwaziEntity(
    val __v: Int,
    val _id: String,
    val attachments: List<Any>,
    val creationDate: Long,
    val documents: List<Any>,
    val editDate: Long,
    val language: String,
    val metadata: Metadata,
    val published: Boolean,
    val relations: List<Any>,
    val sharedId: String,
    val template: String,
    val title: String,
    val user: String
)