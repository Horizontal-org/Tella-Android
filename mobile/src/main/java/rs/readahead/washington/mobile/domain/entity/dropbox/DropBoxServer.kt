package rs.readahead.washington.mobile.domain.entity.dropbox

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType

class DropBoxServer @JvmOverloads constructor(
    id: Long = 0,
    val token: String = "",
) : Server() {
    init {
        serverType = ServerType.DROP_BOX
        setId(id)
        name = "DropBox"
    }
}