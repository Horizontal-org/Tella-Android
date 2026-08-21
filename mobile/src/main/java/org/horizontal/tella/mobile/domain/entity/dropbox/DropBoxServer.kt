package org.horizontal.tella.mobile.domain.entity.dropbox

import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.ServerType

class DropBoxServer @JvmOverloads constructor(
    id: Long = 0,
    var token: String = "",
) : Server() {
    init {
        serverType = ServerType.DROP_BOX
        setId(id)
        name = "Dropbox"
    }
}