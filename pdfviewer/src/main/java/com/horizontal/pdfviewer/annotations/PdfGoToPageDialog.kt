package com.horizontal.pdfviewer.annotations

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import com.horizontal.pdfviewer.R

/**
 * Go-to-Page dialog for the PDF reader (audit-2025-08-19 / Feature 1.B).
 *
 * Shows a small EditText inside an AlertDialog. Validates that the entered
 * number is in `1..pageCount` before enabling the "Go" button. Calls
 * [onGo] with the zero-based page index when the user confirms.
 *
 * 2025-08-20 (audit-fix rev 7): the "Go" / "Cancel" buttons were rendering
 * as 80 % transparent white text on a white dialog background (invisible).
 * The `pdfviewer` module can't depend on the mobile module's
 * `TellaDialogTheme` (would be a circular dependency), so we fix this
 * surgically by tinting the button text colors after `dialog.show()`.
 * This is module-agnostic and works regardless of the host activity's
 * `colorAccent`.
 *
 * The color is `#D6933B` (Tella orange / `wa_orange`) — matches the
 * mobile `TellaDialogTheme` so the visual style is consistent across
 * all dialogs in the app.
 *
 * Kept as a stand-alone object (no activity dependency, no state) so the
 * host activity can simply call `PdfGoToPageDialog.show(...)` from its
 * toolbar item handler.
 */
object PdfGoToPageDialog {

    fun show(
        context: Context,
        pageCount: Int,
        onGo: (pageIndex: Int) -> Unit
    ) {
        if (pageCount <= 0) return

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                px(context, 24), px(context, 16),
                px(context, 24), px(context, 8)
            )
        }

        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = context.getString(R.string.pdf_annot_goto_hint, pageCount)
            maxLines = 1
        }
        container.addView(input)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.pdf_annot_goto_title)
            .setView(container)
            .setPositiveButton(R.string.pdf_annot_goto_action) { d, _ ->
                val n = input.text.toString().trim().toIntOrNull()
                if (n != null && n in 1..pageCount) {
                    onGo(n - 1)
                    d.dismiss()
                }
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .create()

        // Disable the positive button until a valid page is typed.
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val n = s?.toString()?.trim()?.toIntOrNull()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    n != null && n in 1..pageCount
            }
        })

        dialog.show()

        // 2025-08-20 (audit-fix rev 7): tint the button labels so they're
        // visible regardless of the host activity's `colorAccent`. The
        // host activity's theme (AppTheme.NoActionBar) sets
        // `colorAccent = wa_white_80 (#CCFFFFFF)`, and AppCompat AlertDialog
        // buttons are tinted by `?colorAccent` — so by default both buttons
        // render as 80 % transparent white on a white background, i.e.
        // invisible. We override the text color post-show.
        //
        // We use the Tella orange (#D6933B) to match the mobile
        // TellaDialogTheme. The disabled state is grayed out by setting
        // alpha via `setTextColor` with a muted color.
        val accentColor = 0xFFD6933B.toInt()
        val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        positiveBtn?.setTextColor(accentColor)
        negativeBtn?.setTextColor(accentColor)
        // The positive button's disabled state needs a different color so
        // the user can tell it's disabled. We update it via the
        // TextWatcher above so the color follows the enabled state.
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val n = s?.toString()?.trim()?.toIntOrNull()
                val valid = n != null && n in 1..pageCount
                // Already-tinted button — just keep the color in sync with
                // the enabled state so the disabled button reads as muted.
                positiveBtn?.setTextColor(
                    if (valid) accentColor else 0xFF9E9E9E.toInt()
                )
            }
        })
    }

    private fun px(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
