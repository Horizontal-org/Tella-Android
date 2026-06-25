package org.horizontal.tella.mobile.views.fragment.connections

import androidx.annotation.StringRes
import org.horizontal.tella.mobile.R

enum class ConnectionTab(
    val pageIndex: Int,
    @StringRes val titleRes: Int,
) {
    TEMPLATES(0, R.string.Uwazi_Templates_TabTitle),
    DRAFTS(1, R.string.collect_draft_tab_title),
    OUTBOX(2, R.string.collect_outbox_tab_title),
    SUBMITTED(3, R.string.collect_sent_tab_title);

    companion object {
        val reportsTabs = listOf(DRAFTS, OUTBOX, SUBMITTED)
        val uwaziTabs = listOf(TEMPLATES, DRAFTS, OUTBOX, SUBMITTED)

        fun reportsPageIndex(tab: ConnectionTab): Int = reportsTabs.indexOf(tab)
        fun uwaziPageIndex(tab: ConnectionTab): Int = uwaziTabs.indexOf(tab)
    }
}
