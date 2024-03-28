package org.hzontal.shared_ui.bottomsheet

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

fun DialogFragment.showOnce(manager: FragmentManager, tag: String) {
    if (manager.findFragmentByTag(tag) == null) {
        show(manager, tag)
    }
}