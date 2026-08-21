package org.hzontal.shared_ui.security

import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import java.security.MessageDigest
import java.util.Locale

/**
 * 2025-08-20 (audit-fix rev 7): the "Set Quick Delete PIN" dialog's OK /
 * Cancel buttons were rendering as 80 % transparent white text on a white
 * dialog background, i.e. invisible — same root-cause bug as the sticky
 * note editor and the highlight picker.
 *
 * The shared-ui module can't reference the mobile module's
 * `R.style.TellaDialogTheme` (would be a circular dependency), so we
 * resolve the theme attribute `?attr/tellaDialogTheme` at runtime. The
 * mobile module declares `<attr name="tellaDialogTheme" format="reference" />`
 * and the mobile `AppTheme.NoActionBar` resolves it to `@style/TellaDialogTheme`
 * (which overrides `colorAccent` to `wa_orange`). This keeps the dialog
 * themable from any host module without a hard dependency.
 *
 * If the host theme doesn't declare `tellaDialogTheme`, we fall back to
 * `android.R.style.Theme_DeviceDefault_Light_Dialog_Alert` so the buttons
 * are still visible — better than invisible.
 */
object QuickDeletePinManager {
    private const val PREFS_NAME = "tella_quick_delete_pin_v1"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val MIN_LEN = 4
    private const val MAX_LEN = 8

    fun isSet(context: Context): Boolean {
        return prefs(context).getString(KEY_PIN_HASH, "").orEmpty().isNotEmpty()
    }

    fun matches(context: Context, pin: String): Boolean {
        if (pin.isBlank()) return false
        val stored = prefs(context).getString(KEY_PIN_HASH, "").orEmpty()
        if (stored.isEmpty()) return false
        return constantTimeEquals(stored, hashPin(pin))
    }

    fun setPin(context: Context, pin: String) {
        require(pin.length in MIN_LEN..MAX_LEN) { "PIN must be $MIN_LEN..$MAX_LEN digits" }
        prefs(context).edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun clearPin(context: Context) {
        prefs(context).edit().remove(KEY_PIN_HASH).apply()
    }

    /**
     * Builds an [AlertDialog.Builder] whose context is wrapped with the
     * host's dialog theme overlay (resolved via `?attr/tellaDialogTheme`).
     * This makes the OK / Cancel buttons visible (orange on white) instead
     * of inherited 80 % white-on-white.
     *
     * Falls back to the platform default light dialog theme if the host
     * doesn't declare `tellaDialogTheme`.
     */
    private fun themedBuilder(context: Context): AlertDialog.Builder {
        // 2025-08-20 (audit-fix rev 8): resolve the attr id at runtime
        // because shared-ui's R class doesn't always generate the attr
        // reference reliably across AGP versions. Using getIdentifier is
        // slightly slower but works regardless of build configuration.
        val attrId = context.resources.getIdentifier(
            "tellaDialogTheme", "attr", context.packageName
        )
        val tv = TypedValue()
        val resolved = if (attrId != 0) {
            context.theme.resolveAttribute(attrId, tv, true)
        } else false
        val themedContext = if (resolved && tv.resourceId != 0) {
            ContextThemeWrapper(context, tv.resourceId)
        } else {
            // Fallback: use the AppCompat light dialog alert theme so the
            // buttons are at least visible. This shouldn't happen because
            // the mobile AppTheme.NoActionBar theme inherits the attr
            // from the shared-ui attrs.xml, but we guard anyway.
            ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        }
        return AlertDialog.Builder(themedContext)
    }

    fun showSetPinDialog(context: Context, onSaved: () -> Unit = {}, onCancelled: () -> Unit = {}) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(context, 24), px(context, 16), px(context, 24), px(context, 8))
        }
        val pin = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = str(context, "quick_delete_pin_enter_pin", "Enter 4-8 digit PIN")
            maxLines = 1
        }
        val confirm = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = str(context, "quick_delete_pin_confirm", "Re-enter PIN")
            maxLines = 1
        }
        container.addView(pin)
        container.addView(confirm)

        // 2025-08-20 (audit-fix rev 7): use themedBuilder() instead of
        // plain AlertDialog.Builder(context) so the OK / Cancel buttons
        // are visible. Was: AlertDialog.Builder(context).
        themedBuilder(context)
            .setTitle(str(context, "quick_delete_pin_set", "Set Quick Delete PIN"))
            .setMessage(str(context, "quick_delete_pin_summary", "A separate PIN that triggers Tella's Quick Delete sequence when entered at the lock screen."))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val p = pin.text.toString().trim()
                val c = confirm.text.toString().trim()
                if (p.length !in MIN_LEN..MAX_LEN) {
                    toast(context, str(context, "quick_delete_pin_wrong_length", "PIN must be 4-8 digits"))
                    onCancelled()
                    return@setPositiveButton
                }
                if (p != c) {
                    toast(context, str(context, "quick_delete_pin_mismatch", "PINs do not match"))
                    onCancelled()
                    return@setPositiveButton
                }
                setPin(context, p)
                toast(context, str(context, "quick_delete_pin_set_ok", "Quick Delete PIN saved"))
                onSaved()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancelled() }
            .create()
            .show()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) { diff = diff or (a[i].code xor b[i].code) }
        return diff == 0
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return buildString { for (b in bytes) { append(String.format(Locale.US, "%02x", b)) } }
    }

    private fun px(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
    private fun toast(context: Context, message: String) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    private fun str(context: Context, name: String, default: String): String {
        val res = context.applicationContext.resources
        val id = res.getIdentifier(name, "string", context.applicationContext.packageName)
        return if (id != 0) res.getString(id) else default
    }
}

