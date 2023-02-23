package rs.readahead.washington.mobile.domain.entity.reports

data class FileResult(
    val bucket: String?,
    val fileInfo: FileInfo?,
    val fileName: String?,
    val id: String?,
    val type: String?
)