package org.horizontal.tella.mobile.views.activity.onboarding

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.base_ui.FragmentEdgeToEdge

object OnboardingInsets {

    /**
     * ViewPager slides: top/sides from the pager, bottom from the activity bar (except Welcome).
     */
    fun applyCarouselSlide(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }

    /** Welcome: primary CTA must sit above the system navigation bar. */
    fun applyWelcomeSlide(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }

    /** Full-screen overlays on [R.id.rootOnboard] (lock flow, success, etc.). */
    fun applyFullscreenOverlay(view: View, activity: BaseActivity) {
        FragmentEdgeToEdge.apply(view, activity)
    }
}
