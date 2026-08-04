package org.horizontal.tella.mobile.views.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlin.math.max
import kotlin.math.roundToInt


class ParagraphInlineLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val horizontalGapPx =
        (INLINE_GAP_DP * resources.displayMetrics.density).roundToInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        val leading = getChildAt(LEADING) ?: return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val inline = getChildAt(INLINE)
        val trailing = getChildAt(TRAILING)

        leading.measure(childWidthSpec, wrapHeightSpec)
        inline?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            wrapHeightSpec
        )
        trailing?.measure(childWidthSpec, wrapHeightSpec)

        val inlineFitsOnLastLine = inline != null && fitsOnLastLine(leading, inline)
        var height = leading.measuredHeight
        if (inline != null && !inlineFitsOnLastLine) {
            height += inline.measuredHeight
        }
        if (trailing != null) {
            height += trailing.measuredHeight
        }

        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val leading = getChildAt(LEADING) ?: return
        val inline = getChildAt(INLINE)
        val trailing = getChildAt(TRAILING)

        leading.layout(0, 0, leading.measuredWidth, leading.measuredHeight)

        var nextTop = leading.measuredHeight
        if (inline != null) {
            if (fitsOnLastLine(leading, inline)) {
                layoutInlineOnLastLine(leading, inline)
            } else {
                inline.layout(0, nextTop, inline.measuredWidth, nextTop + inline.measuredHeight)
                nextTop += inline.measuredHeight
            }
            nextTop = max(nextTop, inline.bottom)
        }

        trailing?.layout(0, nextTop, trailing.measuredWidth, nextTop + trailing.measuredHeight)
    }

    private fun layoutInlineOnLastLine(leading: View, inline: View) {
        val textView = leading as? TextView
        val textLayout = textView?.layout
        if (textView == null || textLayout == null || textLayout.lineCount == 0) {
            inline.layout(
                0,
                leading.measuredHeight,
                inline.measuredWidth,
                leading.measuredHeight + inline.measuredHeight
            )
            return
        }

        val lastLine = textLayout.lineCount - 1
        val left = textView.paddingStart +
            textLayout.getLineWidth(lastLine).roundToInt() +
            horizontalGapPx
        val baseline = textView.paddingTop + textLayout.getLineBaseline(lastLine)
        val top = (baseline - inline.baseline).coerceAtLeast(0)
        inline.layout(left, top, left + inline.measuredWidth, top + inline.measuredHeight)
    }

    private fun fitsOnLastLine(leading: View, inline: View): Boolean {
        val textView = leading as? TextView ?: return false
        val textLayout = textView.layout ?: return false
        if (textLayout.lineCount == 0) return false

        val lastLine = textLayout.lineCount - 1
        val usedWidth = textView.paddingStart + textLayout.getLineWidth(lastLine) + textView.paddingEnd
        val available = leading.measuredWidth - usedWidth - horizontalGapPx
        return inline.measuredWidth <= available
    }

    companion object {
        private const val LEADING = 0
        private const val INLINE = 1
        private const val TRAILING = 2
        private const val INLINE_GAP_DP = 4f
    }
}
