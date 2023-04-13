package rs.readahead.washington.mobile.domain.entity.reports

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import java.io.Serializable

class TellaReportServer @JvmOverloads constructor(
    id: Long = 0,
    var accessToken: String = "",
    var projectId: String = "",
    var projectName: String = "",
    var projectSlug: String = "",
    var isActivatedMetadata: Boolean = false,
    var isActivatedBackgroundUpload: Boolean = false,
    var isAutoUpload: Boolean = false,
    var isAutoDelete: Boolean = false
) :
    Server(), Serializable {
    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other !is TellaReportServer) {
            return false
        }
        return (id == other.id && projectId == other.projectId)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    companion object {
        @JvmField
        val NONE = TellaReportServer()
    }

    init {
        serverType = ServerType.TELLA_UPLOAD
        setId(id)
    }
}