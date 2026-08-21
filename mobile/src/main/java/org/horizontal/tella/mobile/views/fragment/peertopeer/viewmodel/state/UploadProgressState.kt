package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state

import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus

data class UploadProgressState(
    val title: String,
    val percent: Int,
    val sessionStatus: SessionStatus,
    val files: List<ProgressFile>
)