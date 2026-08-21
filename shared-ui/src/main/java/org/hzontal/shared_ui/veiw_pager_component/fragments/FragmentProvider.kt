package org.hzontal.shared_ui.veiw_pager_component.fragments

import androidx.fragment.app.Fragment

interface FragmentProvider {
    fun createFragment(position: Int): Fragment
}