package org.horizontal.tella.mobile.util


import android.app.Activity
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences


object ThemeStyleManager {
    /**
     * Based on the values of the theme switches combination
     * returns the appropriate style.
     *
     * @param activity an Activity
     */
    fun getThemeStyle(activity: Activity): Int {
        //This should be more elegant
        when (activity.localClassName) {
            "org.horizontal.tella.mobile.views.activity.camera.CameraActivity" -> {
                return getThemeStyleBasedOnPreferences(
                    R.style.CameraTheme,
                    R.style.CameraTheme_LineSpacing,
                    R.style.CameraTheme_Justify,
                    R.style.CameraTheme_LineSpacingJustify
                )
            }

            "org.horizontal.tella.mobile.views.activity.viewer.AudioPlayActivity" -> {
                return getThemeStyleBasedOnPreferences(
                    R.style.PlayerTheme,
                    R.style.PlayerTheme_LineSpacing,
                    R.style.PlayerTheme_Justify,
                    R.style.PlayerTheme_LineSpacingJustify
                )
            }

            else -> {
                return getThemeStyleBasedOnPreferences(
                    R.style.AppTheme_NoActionBar,
                    R.style.AppTheme_NoActionBar_LineSpacing,
                    R.style.AppTheme_NoActionBar_Justify,
                    R.style.AppTheme_NoActionBar_LineSpacingJustify
                )
            }
        }
    }

    private fun getThemeStyleBasedOnPreferences(
        defaultStyle: Int,
        lineSpacingStyle: Int,
        justifyStyle: Int,
        lineSpacingJustifyStyle: Int
    ): Int {
        val isTextSpacingEnabled = Preferences.isTextSpacing()
        val isTextJustificationEnabled = Preferences.isTextJustification()

        return when {
            isTextSpacingEnabled && isTextJustificationEnabled -> lineSpacingJustifyStyle
            isTextSpacingEnabled -> lineSpacingStyle
            isTextJustificationEnabled -> justifyStyle
            else -> defaultStyle
        }
    }

}