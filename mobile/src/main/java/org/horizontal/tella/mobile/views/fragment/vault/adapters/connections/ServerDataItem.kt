package org.horizontal.tella.mobile.views.fragment.vault.adapters.connections

import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.ServerType

data class ServerDataItem(val servers: List<Server>, val type: ServerType)