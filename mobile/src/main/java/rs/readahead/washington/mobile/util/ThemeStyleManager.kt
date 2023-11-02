package rs.readahead.washington.mobile.util


import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences

class ThemeStyleManager private constructor() {
    public fun getThemeStyle(): Int {
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