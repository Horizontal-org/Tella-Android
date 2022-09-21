package rs.readahead.washington.mobile.domain.entity.reports

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import java.io.Serializable

class TellaReportServer @JvmOverloads constructor(id: Long = 0) : Server(), Serializable {
    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (this === obj) {
            return true
        }
        if (obj !is TellaReportServer) {
            return false
        }
        return id == obj.id
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