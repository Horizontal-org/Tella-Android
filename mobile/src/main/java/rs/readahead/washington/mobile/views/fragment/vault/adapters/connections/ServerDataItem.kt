package rs.readahead.washington.mobile.views.fragment.vault.adapters.connections

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType

data class ServerDataItem(val servers: List<Server>, val type: ServerType)