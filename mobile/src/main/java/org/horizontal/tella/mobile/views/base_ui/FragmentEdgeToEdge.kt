package org.horizontal.tella.mobile.views.base_ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.horizontal.tella.mobile.R

internal object FragmentEdgeToEdge {

    fun apply(view: View, activity: BaseActivity) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (activity is AppBarInsetActivity) {
                v.setPadding(bars.left, 0, bars.right, bars.bottom)
            } else {
                applyFragmentInsets(v, bars)
            }
            insets
        }
    }

    private fun applyFragmentInsets(view: View, bars: androidx.core.graphics.Insets) {
        val appBar = view.findViewById<View>(R.id.appbar)
        if (appBar != null) {
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            appBar.setPadding(
                appBar.paddingLeft,
                bars.top,
                appBar.paddingRight,
                appBar.paddingBottom
            )
            return
        }

        val toolbar = view.findViewById<View>(R.id.toolbar)
        if (toolbar != null && toolbar.parent === view) {
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            applyTopInsetAsMargin(toolbar, bars.top)
            return
        }

        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
    }

    private fun applyTopInsetAsMargin(view: View, topInset: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (params.topMargin != topInset) {
            params.topMargin = topInset
            view.layoutParams = params
        }
    }
}
