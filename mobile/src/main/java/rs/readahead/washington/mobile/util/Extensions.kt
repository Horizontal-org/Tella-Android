package rs.readahead.washington.mobile.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import org.cleaninsights.sdk.Campaign
import org.cleaninsights.sdk.CleanInsights
import org.cleaninsights.sdk.CleanInsightsConfiguration
import timber.log.Timber
import java.net.URL


fun View.setMargins(
    leftMarginDp: Int? = null,
    topMarginDp: Int? = null,
    rightMarginDp: Int? = null,
    bottomMarginDp: Int? = null
) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        leftMarginDp?.run { params.leftMargin = this.dpToPx(context) }
        topMarginDp?.run { params.topMargin = this.dpToPx(context) }
        rightMarginDp?.run { params.rightMargin = this.dpToPx(context) }
        bottomMarginDp?.run { params.bottomMargin = this.dpToPx(context) }
        requestLayout()
    }
}

fun Int.dpToPx(context: Context): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics).toInt()
}

fun Window.changeStatusColor(context: Context, color: Int) {
    if (Build.VERSION.SDK_INT >= 21) {
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        statusBarColor = context.resources.getColor(color)
    }
}

fun createCleanInsightsInstance(context: Context, startDate: Long): CleanInsights? {
    return try {
        val endDate = startDate + (7 * 86400)
        val cleanInsightsConfiguration = CleanInsightsConfiguration(
            URL("https://analytics.wearehorizontal.org/ci/cleaninsights.php"),
            3,
            mapOf(CleanInsightUtils.CAMPAIGN_ID to Campaign(startDate, endDate, 1L))
        )
        CleanInsights(cleanInsightsConfiguration, context.filesDir)
    } catch (e: Exception) {
        Timber.e("createCleanInsightsInstance Exception ${e.message}")
        e.printStackTrace()
        null
    }
}

fun Activity.isKeyboardOpened(): Boolean {
    val r = Rect()

    val activityRoot = getActivityRoot()
    val visibleThreshold = dip(100)

    activityRoot.getWindowVisibleDisplayFrame(r)

    val heightDiff = activityRoot.rootView.height - r.height()

    return heightDiff > visibleThreshold;
}

fun Activity.getActivityRoot(): View {
    return (findViewById<ViewGroup>(android.R.id.content)).getChildAt(0);
}

fun dip(value: Int): Int {
    return (value * Resources.getSystem().displayMetrics.density).toInt()
}