package org.horizontal.tella.mobile.views.fragment.dropbox.data

import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

data class RefreshDropBoxServer(val isFromDropBoxSendView: Boolean = false, val server: DropBoxServer)
