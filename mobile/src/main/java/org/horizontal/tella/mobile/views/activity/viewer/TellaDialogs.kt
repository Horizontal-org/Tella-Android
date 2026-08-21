package org.horizontal.tella.mobile.views.activity.viewer

import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import androidx.annotation.StyleRes
import org.horizontal.tella.mobile.R

/**
 * 2025-08-20 (audit-fix): Centralised dialog builder.
 *
 * ## Why this exists
 *
 * Every audit-era dialog was constructed with `AlertDialog.Builder(context)`
 * (no theme override). The activity theme `AppTheme.NoActionBar` sets
 * `colorAccent = wa_white_80 (#CCFFFFFF)`, and AppCompat AlertDialog
 * buttons are tinted by `?colorAccent` — so every positive/negative/neutral
 * button label rendered as 80% transparent white on a white dialog
 * background, i.e. invisible. The user reported this as "Save / OK / Close
 * buttons are not visible" on:
 *
 *  - the sticky note editor (Save / Cancel / Delete)
 *  - the highlight brush picker (Apply / Cancel)
 *  - the sticky note style picker (Apply / Cancel)
 *  - the annotations list dialog (Cancel)
 *  - the "copy text" long-press menu (Cancel)
 *  - the "clear all annotations" confirm dialog (Delete / Cancel)
 *  - the Settings > Security "change/remove Quick Delete PIN" picker (Cancel)
 *  - the "Set Quick Delete PIN" dialog (OK / Cancel) — see [QuickDeletePinManager]
 *  - the "Secure wipe" prompt (Secure wipe / Skip) — see [SecureWipeDialog]
 *
 * ## Fix
 *
 * Every dialog builder is now obtained via `TellaDialogs.builder(context)`
 * which wraps the context in `ContextThemeWrapper(context, R.style.TellaDialogTheme)`
 * before constructing the `AlertDialog.Builder`. The theme overlay overrides
 * only `colorAccent` (to `wa_orange`) and `android:textColorPrimary` (to
 * `wa_darker_gray`); the dialog background stays the AppCompat default white.
 *
 * ## Usage
 *
 * Replace:
 *   `AlertDialog.Builder(context).setTitle(...).show()`
 * with:
 *   `TellaDialogs.builder(context).setTitle(...).show()`
 *
 * The returned type is `AlertDialog.Builder`, so the rest of the call chain
 * (`.setTitle`, `.setView`, `.setPositiveButton`, `.show()`) is unchanged.
 *
 * ## Why not override `colorAccent` directly in `AppTheme.NoActionBar`?
 *
 * That would fix every dialog at once, but it would also change the tint of
 * every `SwitchCompat`, `CheckBox`, `EditText` cursor, and any other widget
 * that uses `?colorAccent` across the whole app. The Tella design system
 * relies on `colorAccent = wa_white_80` for those widgets on the dark
 * purple background. Using a `ThemeOverlay` scoped to dialogs is the
 * surgical fix.
 */
object TellaDialogs {

    /**
     * Returns an `AlertDialog.Builder` whose context is wrapped with
     * [R.style.TellaDialogTheme], so every button label is rendered in a
     * visible color regardless of the host activity's `colorAccent`.
     */
    fun builder(context: Context): AlertDialog.Builder {
        val themedContext = androidx.appcompat.view.ContextThemeWrapper(
            context,
            resolveDialogTheme(context)
        )
        return AlertDialog.Builder(themedContext)
    }

    /**
     * Returns the dialog theme resource id. Currently always
     * [R.style.TellaDialogTheme], but kept as a resolver so future code
     * can swap to a dark-on-light or light-on-dark variant based on the
     * host activity's actual theme (e.g. for `PlayerTheme`).
     */
    @StyleRes
    private fun resolveDialogTheme(context: Context): Int {
        // Look up `?attr/tellaDialogTheme` if a future activity theme declares
        // it; otherwise fall back to the canonical light-overlay theme.
        val tv = TypedValue()
        val resolved = context.theme.resolveAttribute(
            R.attr.tellaDialogTheme, tv, true
        )
        return if (resolved && tv.resourceId != 0) tv.resourceId
        else R.style.TellaDialogTheme
    }
}
