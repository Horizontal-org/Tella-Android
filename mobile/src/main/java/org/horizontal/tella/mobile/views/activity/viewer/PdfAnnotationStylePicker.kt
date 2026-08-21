package org.horizontal.tella.mobile.views.activity.viewer

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.horizontal.pdfviewer.annotations.PdfAnnotation
import org.horizontal.tella.mobile.R

/**
 * 2025-08-20 (audit-fix rev 7): Color + size picker for PDF annotations.
 *
 * ## Changes in rev 7
 *
 * 1. **Brush sizes reduced by 70 %.** The user reported that even the
 *    "S" size of the highlight brush was wider than a line of text, and
 *    "L" covered three lines. The original multipliers were:
 *
 *      width  (fraction of page width) : S=0.20, M=0.35, L=0.50
 *      height (× 24 dp line-height)    : S=0.70, M=1.00, L=1.50
 *      sticky (× 32 dp pushpin radius) : S=0.70, M=1.00, L=1.50
 *
 *    Multiplying every value by 0.30 gives:
 *
 *      width  : S=0.06, M=0.105, L=0.15
 *      height : S=0.21, M=0.30,  L=0.45
 *      sticky : S=0.21, M=0.30,  L=0.45
 *
 *    "S" now highlights a single line at ~6 % of page width (about 2–3
 *    words), "M" matches a typical text line at ~10 %, and "L" still
 *    spans ~15 % — large enough for emphasis, small enough to read the
 *    text under it.
 *
 * 2. **"Save" → "Apply" on the picker's positive button.** The picker
 *    does not persist anything to disk; tapping the button activates
 *    the chosen highlight / sticky note mode. "Apply" describes that
 *    effect; "Save" was misleading. The sticky-note *text* editor still
 *    uses "Save" because that dialog really does save the typed text
 *    into the annotation store.
 *
 * 3. **All dialogs built via [TellaDialogs.builder]** so the Apply /
 *    Cancel buttons are rendered in `wa_orange` instead of the inherited
 *    `wa_white_80` (white-on-white invisible).
 *
 * The size buttons themselves were already fine (gray background, dark
 * text) — only the dialog buttons at the bottom of the picker had the
 * white-on-white bug.
 */
object PdfAnnotationStylePicker {

    data class HighlightStyleResult(
        val color: Int,
        val widthIndex: Int,   // 0=S, 1=M, 2=L
        val heightIndex: Int   // 0=S, 1=M, 2=L
    )

    data class StickyStyleResult(
        val color: Int,
        val sizeIndex: Int  // 0=S, 1=M, 2=L
    )

    private val HIGHLIGHT_COLORS = intArrayOf(
        Color.parseColor("#FFEB3B"),  // yellow
        Color.parseColor("#66BB6A"),   // green
        Color.parseColor("#42A5F5"),   // blue
        Color.parseColor("#EF5350"),   // red
        Color.parseColor("#AB47BC"),   // purple
        Color.parseColor("#FF9800")    // orange
    )

    private val STICKY_COLORS = intArrayOf(
        Color.parseColor("#E54A2D"),  // red-orange (default pushpin)
        Color.parseColor("#FFA726"),  // amber
        Color.parseColor("#66BB6A"),  // green
        Color.parseColor("#42A5F5"),  // blue
        Color.parseColor("#AB47BC"),  // purple
        Color.parseColor("#FFEB3B")   // yellow
    )

    private val SIZE_LABELS = arrayOf("S", "M", "L")

    // 2025-08-20 (audit-fix rev 7): reduced by 70 % from
    //   SIZE_MULTIPLIERS  = [0.7, 1.0, 1.5]
    //   WIDTH_MULTIPLIERS  = [0.20, 0.35, 0.50]
    // to
    //   SIZE_MULTIPLIERS  = [0.21, 0.30, 0.45]
    //   WIDTH_MULTIPLIERS  = [0.06, 0.105, 0.15]
    private val SIZE_MULTIPLIERS = floatArrayOf(0.21f, 0.30f, 0.45f)
    private val WIDTH_MULTIPLIERS = floatArrayOf(0.06f, 0.105f, 0.15f)  // fraction of page width

    fun showHighlightPicker(
        context: Context,
        currentColor: Int,
        currentWidthIndex: Int,
        currentHeightIndex: Int,
        onResult: (HighlightStyleResult) -> Unit
    ) {
        var selectedColor = currentColor
        var selectedWidth = currentWidthIndex
        var selectedHeight = currentHeightIndex

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        // 2025-08-20 (audit-fix rev 8): declare the view lists UP FRONT so
        // the color swatch click handler can reference them without a
        // forward-reference error. Previously the swatch click handler
        // called `refreshSizeButtons(widthViews, ...)` before `widthViews`
        // was declared, causing a compile error.
        val widthViews = mutableListOf<View>()
        val heightViews = mutableListOf<View>()

        // --- Color section ---
        container.addView(TextView(context).apply {
            text = context.getString(R.string.pdf_annot_picker_color)
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 0, 0, 16)
        })
        val colorGrid = GridLayout(context).apply {
            columnCount = 6
            rowCount = 1
            useDefaultMargins = true
        }
        val colorViews = mutableListOf<View>()
        HIGHLIGHT_COLORS.forEach { color ->
            val swatch = View(context).apply {
                setBackgroundColor(color)
                val size = (40 * resources.displayMetrics.density).toInt()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                if (color == currentColor) {
                    alpha = 1.0f
                    scaleX = 1.2f
                    scaleY = 1.2f
                } else {
                    alpha = 0.7f
                }
                setOnClickListener {
                    selectedColor = color
                    colorViews.forEach { v -> v.alpha = 0.7f; v.scaleX = 1.0f; v.scaleY = 1.0f }
                    alpha = 1.0f; scaleX = 1.2f; scaleY = 1.2f
                    // 2025-08-20 (audit-fix rev 7): also refresh the size
                    // buttons so the active swatch color is reflected on
                    // the selected S/M/L pill — gives the user immediate
                    // feedback that the new color is now active.
                    refreshSizeButtons(widthViews, selectedWidth, selectedColor)
                    refreshSizeButtons(heightViews, selectedHeight, selectedColor)
                }
            }
            colorViews.add(swatch)
            colorGrid.addView(swatch)
        }
        container.addView(colorGrid)

        // --- Width section ---
        container.addView(TextView(context).apply {
            text = context.getString(R.string.pdf_annot_picker_width)
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 32, 0, 16)
        })
        val widthRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // widthViews is declared at the top of the function now (rev 8 fix).
        SIZE_LABELS.forEachIndexed { idx, label ->
            val btn = TextView(context).apply {
                text = label
                textSize = 18f
                gravity = Gravity.CENTER
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad / 2, pad, pad / 2)
                if (idx == currentWidthIndex) {
                    setBackgroundColor(selectedColor)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    setTextColor(Color.parseColor("#424242"))
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(8, 0, 8, 0)
                layoutParams = lp
                setOnClickListener {
                    selectedWidth = idx
                    refreshSizeButtons(widthViews, idx, selectedColor)
                }
            }
            widthViews.add(btn)
            widthRow.addView(btn)
        }
        container.addView(widthRow)

        // --- Height section ---
        container.addView(TextView(context).apply {
            text = context.getString(R.string.pdf_annot_picker_height)
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 32, 0, 16)
        })
        val heightRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // heightViews is declared at the top of the function now (rev 8 fix).
        SIZE_LABELS.forEachIndexed { idx, label ->
            val btn = TextView(context).apply {
                text = label
                textSize = 18f
                gravity = Gravity.CENTER
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad / 2, pad, pad / 2)
                if (idx == currentHeightIndex) {
                    setBackgroundColor(selectedColor)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    setTextColor(Color.parseColor("#424242"))
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(8, 0, 8, 0)
                layoutParams = lp
                setOnClickListener {
                    selectedHeight = idx
                    refreshSizeButtons(heightViews, idx, selectedColor)
                }
            }
            heightViews.add(btn)
            heightRow.addView(btn)
        }
        container.addView(heightRow)

        // 2025-08-20 (audit-fix rev 7): TellaDialogs.builder applies the
        // TellaDialogTheme overlay so the Apply / Cancel buttons render in
        // wa_orange (#D6933B) instead of inherited wa_white_80. "Apply"
        // replaces "Save" because the picker activates a mode rather than
        // persisting any data — the user reported "save doesn't suitable
        // it should be active mode much more meaningful".
        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_highlight_picker_title)
            .setView(container)
            .setPositiveButton(R.string.pdf_annot_dialog_apply) { d, _ ->
                onResult(HighlightStyleResult(selectedColor, selectedWidth, selectedHeight))
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    fun showStickyNotePicker(
        context: Context,
        currentColor: Int,
        currentSizeIndex: Int,
        onResult: (StickyStyleResult) -> Unit
    ) {
        var selectedColor = currentColor
        var selectedSize = currentSizeIndex

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        // 2025-08-20 (audit-fix rev 8): declare sizeViews UP FRONT so the
        // color swatch click handler can reference it (was a forward-ref
        // compile error).
        val sizeViews = mutableListOf<View>()

        // --- Color section ---
        container.addView(TextView(context).apply {
            text = context.getString(R.string.pdf_annot_picker_color)
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 0, 0, 16)
        })
        val colorGrid = GridLayout(context).apply {
            columnCount = 6
            rowCount = 1
            useDefaultMargins = true
        }
        val colorViews = mutableListOf<View>()
        STICKY_COLORS.forEach { color ->
            val swatch = View(context).apply {
                setBackgroundColor(color)
                val size = (40 * resources.displayMetrics.density).toInt()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                if (color == currentColor) {
                    alpha = 1.0f; scaleX = 1.2f; scaleY = 1.2f
                } else {
                    alpha = 0.7f
                }
                setOnClickListener {
                    selectedColor = color
                    colorViews.forEach { v -> v.alpha = 0.7f; v.scaleX = 1.0f; v.scaleY = 1.0f }
                    alpha = 1.0f; scaleX = 1.2f; scaleY = 1.2f
                    refreshSizeButtons(sizeViews, selectedSize, selectedColor)
                }
            }
            colorViews.add(swatch)
            colorGrid.addView(swatch)
        }
        container.addView(colorGrid)

        // --- Size section ---
        container.addView(TextView(context).apply {
            text = context.getString(R.string.pdf_annot_picker_size)
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setPadding(0, 32, 0, 16)
        })
        val sizeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // sizeViews is declared at the top of the function now (rev 8 fix).
        SIZE_LABELS.forEachIndexed { idx, label ->
            val btn = TextView(context).apply {
                text = label
                textSize = 18f
                gravity = Gravity.CENTER
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad / 2, pad, pad / 2)
                if (idx == currentSizeIndex) {
                    setBackgroundColor(selectedColor)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    setTextColor(Color.parseColor("#424242"))
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(8, 0, 8, 0)
                layoutParams = lp
                setOnClickListener {
                    selectedSize = idx
                    refreshSizeButtons(sizeViews, idx, selectedColor)
                }
            }
            sizeViews.add(btn)
            sizeRow.addView(btn)
        }
        container.addView(sizeRow)

        TellaDialogs.builder(context)
            .setTitle(R.string.pdf_annot_sticky_picker_title)
            .setView(container)
            .setPositiveButton(R.string.pdf_annot_dialog_apply) { d, _ ->
                onResult(StickyStyleResult(selectedColor, selectedSize))
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    /**
     * Helper that refreshes a row of S/M/L pill buttons so the selected
     * one takes the active color and the others go back to the gray
     * inactive style. Factored out of the per-button click listeners so
     * we can also call it from the color swatch click handler (so the
     * active pill re-tints to the newly picked color immediately).
     */
    private fun refreshSizeButtons(views: List<View>, selectedIndex: Int, activeColor: Int) {
        views.forEachIndexed { idx, v ->
            (v as? TextView)?.apply {
                if (idx == selectedIndex) {
                    setBackgroundColor(activeColor)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    setTextColor(Color.parseColor("#424242"))
                }
            }
        }
    }

    fun widthMultiplier(widthIndex: Int): Float = WIDTH_MULTIPLIERS.getOrElse(widthIndex) { 0.105f }
    fun heightMultiplier(heightIndex: Int): Float = SIZE_MULTIPLIERS.getOrElse(heightIndex) { 0.30f }
    fun sizeMultiplier(sizeIndex: Int): Float = SIZE_MULTIPLIERS.getOrElse(sizeIndex) { 0.30f }
}
