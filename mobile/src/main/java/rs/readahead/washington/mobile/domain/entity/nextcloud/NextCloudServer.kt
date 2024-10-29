package rs.readahead.washington.mobile.domain.entity.nextcloud

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import java.io.Serializable

class NextCloudServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    userId: String = ""
) : Server(), Serializable {


    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    companion object {
        @JvmField
        val NONE = TellaReportServer()
    }

    init {
        serverType = ServerType.NEXTCLOUD
        setId(id)
    }
}