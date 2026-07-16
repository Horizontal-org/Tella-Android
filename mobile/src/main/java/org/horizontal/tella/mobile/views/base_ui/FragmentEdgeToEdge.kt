package org.horizontal.tella.mobile.views.base_ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.horizontal.tella.mobile.R

internal object FragmentEdgeToEdge {

    /**
     * Applies edge-to-edge system-bar insets to [view].
     *
     * @param applyImeInset when true, the keyboard (IME) inset is folded into the bottom
     * padding so a bottom-pinned action bar rides above the keyboard. This resizes the
     * fragment root when the keyboard opens, so only screens that host a bottom action bar
     * over a scrollable form should opt in. Defaults to false, which keeps the whole screen
     * from being pushed up when a text field is focused.
     */
    fun apply(view: View, activity: BaseActivity, applyImeInset: Boolean = false) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (applyImeInset) {
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                maxOf(systemBars.bottom, ime.bottom)
            } else {
                systemBars.bottom
            }

            if (activity is AppBarInsetActivity) {
                v.setPadding(systemBars.left, 0, systemBars.right, bottomInset)
            } else {
                applyFragmentInsets(v, systemBars, bottomInset)
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }

    private fun applyFragmentInsets(
        view: View,
        systemBars: androidx.core.graphics.Insets,
        bottomInset: Int,
    ) {
        val appBar = view.findViewById<View>(R.id.appbar)
        if (appBar != null) {
            view.setPadding(systemBars.left, 0, systemBars.right, bottomInset)
            appBar.setPadding(
                appBar.paddingLeft,
                systemBars.top,
                appBar.paddingRight,
                appBar.paddingBottom
            )
            return
        }

        val toolbar = view.findViewById<View>(R.id.toolbar)
        if (toolbar != null) {
            view.setPadding(systemBars.left, 0, systemBars.right, bottomInset)
            applyTopInset(toolbar, systemBars.top)
            return
        }

        view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
    }

    private fun applyTopInset(view: View, topInset: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
        if (params != null) {
            if (params.topMargin != topInset) {
                params.topMargin = topInset
                view.layoutParams = params
            }
            return
        }
        if (view.paddingTop != topInset) {
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
        }
    }
}
