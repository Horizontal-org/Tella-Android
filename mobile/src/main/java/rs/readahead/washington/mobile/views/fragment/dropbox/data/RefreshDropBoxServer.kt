package rs.readahead.washington.mobile.views.fragment.dropbox.data

import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer

data class RefreshDropBoxServer(val isFromDropBoxSendView: Boolean = false, val server: DropBoxServer)
