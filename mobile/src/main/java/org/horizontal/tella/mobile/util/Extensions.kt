package org.horizontal.tella.mobile.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
/*import org. cleaninsights.sdk.Campaign
import org.cleaninsights.sdk.CleanInsights
import org.cleaninsights.sdk.CleanInsightsConfiguration*/
import timber.log.Timber


/**
 * function that converts data from json to object
 * @param classMapper the class of T.
 * @param <T> the type of the desired object.
 * @return an object of type T from the string.
 */
fun <T> String.fromJsonToObject(classMapper: Class<T>): T? {
    return try {
        Gson().fromJson(this, classMapper)
    } catch (e: JsonParseException) {
        Timber.e(e)
        null
    }
}

fun <T> String.fromJsonToObjectList(clazz: Class<T>?): List<T>? {
    return try {
        val typeOfT = TypeToken.getParameterized(MutableList::class.java, clazz).type
        return Gson().fromJson(this, typeOfT)
    } catch (e: JsonParseException) {
        Timber.e(e)
        null
    }
}

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
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    statusBarColor = context.resources.getColor(color)
}

fun View.setTint(@ColorRes colorRes: Int) {
    background.setTintList(
        ContextCompat.getColorStateList(context, colorRes)
    );
}

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.configureAppBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        outlineProvider = null
    } else {
        bringToFront()
    }
}

fun ImageView.setCheckDrawable(drawableRes: Int, context: Context) {
    val drawable = ContextCompat.getDrawable(context, drawableRes)
    setImageDrawable(drawable)
}

fun FragmentManager.setupForAccessibility(context: Context) {
    if (context.isScreenReaderOn())
        addOnBackStackChangedListener {
            val lastFragmentWithView = fragments.last { it.view != null }
            for (fragment in fragments) {
                if (fragment == lastFragmentWithView) {
                    fragment.view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                } else {
                    fragment.view?.importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
            }
        }
}

fun Context.isScreenReaderOn(): Boolean {
    val accessibilityManager =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    if (accessibilityManager.isEnabled) {
        val serviceInfoList =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
        if (serviceInfoList.isNotEmpty())
            return true
    }
    return false
}

fun NavController.navigateSafe(destinationId: Int, bundle: Bundle? = null) {
    navigate(destinationId, bundle,)
}

