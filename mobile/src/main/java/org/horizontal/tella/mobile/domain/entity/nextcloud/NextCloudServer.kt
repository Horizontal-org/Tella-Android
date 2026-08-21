package org.horizontal.tella.mobile.domain.entity.nextcloud

import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.ServerType
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import java.io.Serializable

class NextCloudServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    var folderId: String = "",
    var userId: String = "",
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