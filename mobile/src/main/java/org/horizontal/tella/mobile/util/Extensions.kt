package org.horizontal.tella.mobile.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
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

fun Window.changeStatusColor(context: Context, @ColorRes colorRes: Int) {
    val color = ContextCompat.getColor(context, colorRes)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        statusBarColor = color
        // enable edge-to-edge
        setDecorFitsSystemWindows(false)  

        insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        statusBarColor = color
    }
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

fun NavController.navigateSafe(@IdRes destinationId: Int, args: Bundle? = null) {
    val currentNode = currentDestination
    val action = currentNode?.getAction(destinationId)

    if (action != null) {
        navigate(destinationId, args)
    } else {
        val resources = currentNode?.navigatorName?.let {
            context.resources
        }

        val destName = try {
            resources?.getResourceEntryName(destinationId) ?: destinationId.toString()
        } catch (e: Exception) {
            destinationId.toString()
        }
        Timber.w("[NavigationSafe] Skipping navigation to '$destName' from '${currentNode?.label}' – action not found")
    }
}

fun String.formatHash(): String {
    return this
        .take(64) // take only the first 64 characters
        .chunked(4) // split into groups of 4
        .chunked(4) // make 4 lines
        .joinToString("\n") { it.joinToString(" ") }

}

