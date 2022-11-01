package rs.readahead.washington.mobile.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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

fun View.highlightBackground(color: Int) {
    apply {
        setBackgroundColor(color)
    }
}

fun View.setTint(@ColorRes colorRes: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        background.setTintList(
            ContextCompat.getColorStateList(context,colorRes));
    }
}


fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}