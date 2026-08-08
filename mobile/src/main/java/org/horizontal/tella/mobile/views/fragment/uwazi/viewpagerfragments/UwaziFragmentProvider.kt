package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider

const val TEMPLATES_TAB_INDEX = 0

/** Uwazi lists downloaded templates before draft/outbox/submitted, unlike the other connections. */
const val UWAZI_LIST_TAB_OFFSET = 1

class UwaziFragmentProvider : FragmentProvider {

    override fun createFragment(position: Int): Fragment = when (position) {
        TEMPLATES_TAB_INDEX -> TemplatesUwaziFragment()
        1 -> DraftsUwaziFragment()
        2 -> OutboxUwaziFragment()
        3 -> SubmittedUwaziFragment()
        else -> throw IndexOutOfBoundsException("Unexpected Uwazi tab position $position")
    }
}
