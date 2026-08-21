package org.horizontal.tella.mobile.views.activity.viewer

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.horizontal.pdfviewer.annotations.PdfAnnotation
import org.horizontal.tella.mobile.R

/**
 * 2025-08-20 (audit-fix rev 7): all dialogs now go through [TellaDialogs.builder]
 * which wraps the host context in `ContextThemeWrapper(context, R.style.TellaDialogTheme)`.
 *
 * This fixes the "white text on white background" bug reported by the user on
 * the Save / Cancel / Delete buttons of the sticky-note editor, the Apply /
 * Cancel buttons of the highlight + sticky note style pickers, and the Cancel
 * button of the annotations list dialog. The previous audit rev6 fix that
 * swapped `BrightBackgroundDarkLettersDialogTheme` for plain
 * `AlertDialog.Builder(context)` did NOT actually fix the bug because
 * `colorAccent = wa_white_80 (#CCFFFFFF)` is still inherited from
 * `AppTheme.NoActionBar` — AppCompat AlertDialog buttons are tinted by
 * `?colorAccent`, producing 80% transparent white text on a white dialog
 * background. The new `TellaDialogTheme` overlay overrides `colorAccent`
 * to `wa_orange (#D6933B)` so the buttons are visible.
 *
 * The annotation-list dialog also gained a new `onNavigate` callback so the
 * host can jump to the page that an annotation lives on before opening the
 * editor — see the audit notes on `showAnnotationList` below.
 */
object PdfAnnotationDialogs {

    fun showStickyNoteEditor(
        context: Context,
        annotation: PdfAnnotation,
        onSave: (PdfAnnotation) -> Unit,
        onDelete: (PdfAnnotation) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pdf_sticky_note, null)
        val input = dialogView.findViewById<EditText>(R.id.stickyNoteInput)
        input.setText(annotation.text)
        input.hint = context.getString(R.string.pdf_annot_dialog_edit_hint)
        input.selectAll()
        input.requestFocus()

        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_dialog_edit_title)
            .setView(dialogView)
            .setPositiveButton(R.string.pdf_annot_dialog_save) { d, _ ->
                val updated = annotation.copy(text = input.text.toString(), updatedAt = System.currentTimeMillis())
                onSave(updated)
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .setNeutralButton(R.string.pdf_annot_dialog_delete) { d, _ ->
                onDelete(annotation)
                d.dismiss()
            }
            .show()
    }

    fun showDeleteConfirmation(
        context: Context,
        annotation: PdfAnnotation,
        onConfirm: (PdfAnnotation) -> Unit
    ) {
        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_dialog_delete_title)
            .setMessage(R.string.pdf_annot_dialog_delete_message)
            .setPositiveButton(R.string.pdf_annot_dialog_delete) { d, _ ->
                onConfirm(annotation)
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    fun showHighlightEditor(
        context: Context,
        annotation: PdfAnnotation,
        onDelete: (PdfAnnotation) -> Unit
    ) {
        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_dialog_delete_title)
            .setMessage(R.string.pdf_annot_dialog_delete_message)
            .setPositiveButton(R.string.pdf_annot_dialog_delete) { d, _ ->
                onDelete(annotation)
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    /**
     * Annotations list dialog.
     *
     * 2025-08-20 (audit-fix rev 7): the list already showed page numbers
     * (each row is `"p.{page+1}: {text}"` or `"(page {page+1}, {type})"`).
     * The user reported "clicking a row does not navigate to the page" —
     * that was correct: the previous behavior was to call [onSelected]
     * immediately, which opened the edit/delete dialog without scrolling.
     *
     * Now the dialog asks the host to navigate FIRST (via [onNavigate]),
     * then asks the host what to do with the annotation (via [onSelected]).
     * Both callbacks are optional so existing callers that only pass
     * `onSelected` keep working.
     *
     * The PDFReaderActivity is updated to pass `onNavigate = { ann ->
     * binding.pdfRendererView.scrollToPage(ann.page) }` so tapping a list
     * row first scrolls the PDF to that page, then opens the editor.
     */
    fun showAnnotationList(
        context: Context,
        annotations: List<PdfAnnotation>,
        onSelected: (PdfAnnotation) -> Unit,
        onNavigate: (PdfAnnotation) -> Unit = {}
    ) {
        if (annotations.isEmpty()) {
            TellaDialogs.builder(context)
                .setMessage(R.string.pdf_annot_empty)
                .setPositiveButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
                .show()
            return
        }
        val sorted = annotations.sortedBy { it.page }
        // 2025-08-20 (audit-fix rev 7): the label now also shows the
        // page number prominently even when the annotation text is blank,
        // so the user can always tell which page they will jump to. The
        // previous "p.N: text" format was fine but easy to miss when the
        // text was empty; we now emit "Page N — text" so the page number
        // reads as the primary identifier.
        val items = sorted.map { ann ->
            val pageLabel = context.getString(R.string.pdf_annot_list_page_label, ann.page + 1)
            val body = if (ann.text.isBlank()) {
                context.getString(R.string.pdf_annot_list_no_text, ann.type.raw)
            } else {
                ann.text.take(60)
            }
            "$pageLabel  —  $body"
        }.toTypedArray()
        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_title_list)
            .setItems(items) { d, idx ->
                val ann = sorted[idx]
                // Navigate first so the page is visible behind the editor
                // dialog that opens next — the user sees both the page
                // they jumped to and the annotation they tapped.
                onNavigate(ann)
                onSelected(ann)
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }
}
