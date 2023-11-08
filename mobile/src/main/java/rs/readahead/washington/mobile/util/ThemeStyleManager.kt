package rs.readahead.washington.mobile.util


import android.app.Activity
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences


class ThemeStyleManager private constructor() {
    /**
     * Based on the values of the theme switches combination
     * returns the appropriate style.
     *
     * @param activity an Activity
     */
    fun getThemeStyle(activity: Activity): Int {
        //This should be more elegant
        when (activity.localClassName) {
            "rs.readahead.washington.mobile.views.activity.camera.CameraActivity" -> {
                if (Preferences.isTextSpacing()) {
                    if (Preferences.isTextJustification()) {
                        return R.style.CameraTheme_LineSpacingJustify
                    } else {
                        return R.style.CameraTheme_LineSpacing
                    }
                } else {
                    if (Preferences.isTextJustification()) {
                        return R.style.CameraTheme_Justify
                    } else {
                        return R.style.CameraTheme
                    }
                }
            }

            "rs.readahead.washington.mobile.views.activity.viewer.AudioPlayActivity" -> {
                if (Preferences.isTextSpacing()) {
                    if (Preferences.isTextJustification()) {
                        return R.style.PlayerTheme_LineSpacingJustify
                    } else {
                        return R.style.PlayerTheme_LineSpacing
                    }
                } else {
                    if (Preferences.isTextJustification()) {
                        return R.style.PlayerTheme_Justify
                    } else {
                        return R.style.PlayerTheme
                    }
                }
            }

            else -> {
                if (Preferences.isTextSpacing()) {
                    if (Preferences.isTextJustification()) {
                        return R.style.AppTheme_NoActionBar_LineSpacingJustify
                    } else {
                        return R.style.AppTheme_NoActionBar_LineSpacing
                    }
                } else {
                    if (Preferences.isTextJustification()) {
                        return R.style.AppTheme_NoActionBar_Justify
                    } else {
                        return R.style.AppTheme_NoActionBar
                    }
                }
            }
        }
    }

    companion object {
        @get:Synchronized
        var instance: ThemeStyleManager? = null
            get() {
                if (field == null) {
                    field = ThemeStyleManager()
                }
                return field
            }
    }
}