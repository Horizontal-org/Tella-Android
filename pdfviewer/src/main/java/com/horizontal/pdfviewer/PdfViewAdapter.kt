package com.horizontal.pdfviewer

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.horizontal.pdfviewer.annotations.PdfAnnotation
import com.horizontal.pdfviewer.annotations.PdfAnnotationOverlayView
import com.horizontal.pdfviewer.databinding.ListItemPdfPageBinding
import com.horizontal.pdfviewer.util.CommonUtils
import com.horizontal.pdfviewer.util.hide
import com.horizontal.pdfviewer.util.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Rajat on 11,July,2020
 *
 * 2025-08-19 (audit): Added [annotationOverlay] binding, [annotations] list,
 * and an [annotationListener] bridge so each rendered page also displays any
 * user-created highlights / sticky-note markers and forwards new gestures
 * up to the host activity.
 */

internal class PdfViewAdapter(
    private val context: Context,
    private val renderer: PdfRendererCore,
    private val pageSpacing: Rect,
    private val enableLoadingForPages: Boolean
) :
    RecyclerView.Adapter<PdfViewAdapter.PdfPageViewHolder>() {

    /** Annotations for the currently open document, keyed by page index. */
    private var annotations: List<PdfAnnotation> = emptyList()

    /** Forwarded to every bound overlay view so gestures reach the host. */
    var annotationListener: PdfAnnotationOverlayView.AnnotationListener? = null
        set(value) { field = value; notifyItemRangeChanged(0, itemCount) }

    /** Mirrors the host's current annotation mode (drag-to-highlight / tap-for-sticky / off). */
    var annotationMode: PdfAnnotationOverlayView.AnnotationMode =
        PdfAnnotationOverlayView.AnnotationMode.OFF
        set(value) { field = value; notifyItemRangeChanged(0, itemCount) }

    /** Tap-handler the host sets so it can open edit / delete dialogs. */
    var annotationTapListener: AnnotationTapListener? = null

    // 2025-08-19 (audit rev6): color + size state forwarded to overlays.
    var highlightColor: Int = PdfAnnotationOverlayView.DEFAULT_HIGHLIGHT_COLOR
    var highlightWidthMultiplier: Float = 0.35f
    var highlightHeightMultiplier: Float = 1.0f
    var stickyNoteColor: Int = PdfAnnotationOverlayView.DEFAULT_STICKY_COLOR
    var stickyNoteSizeMultiplier: Float = 1.0f
    var longPressListener: PdfAnnotationOverlayView.LongPressListener? = null

    // 2025-08-19 (audit rev6): use payload so only overlay updates, NOT page bitmap.
    fun setAnnotations(items: List<PdfAnnotation>) {
        annotations = items
        notifyItemRangeChanged(0, itemCount, PAYLOAD_OVERLAY_ONLY)
    }

    fun refreshAnnotations() {
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        return PdfPageViewHolder(
            ListItemPdfPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return renderer.getPageCount()
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    // 2025-08-19 (audit rev6): partial bind — updates ONLY the overlay, NOT the bitmap.
    override fun onBindViewHolder(
        holder: PdfPageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_OVERLAY_ONLY)) {
            holder.bindOverlayOnly(position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** Surface the host can implement to receive tap events on existing annotations. */
    fun interface AnnotationTapListener {
        fun onAnnotationTapped(annotation: PdfAnnotation)
    }

    inner class PdfPageViewHolder(private val itemBinding: ListItemPdfPageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(position: Int) {
            with(itemBinding) {
                handleLoadingForPage(position)
                if (pageView.width == 0 || pageView.height == 0) {
                    pageView.post { bind(position) }  // Postpone if layout not ready
                    return
                }
                // 2025-08-19 (audit-fix): render at 2x the view width so
                // pinch-zoom has more pixels to work with. Without this the
                // page is rasterised at exactly the device width, which is
                // why zooming in produces blurry text — the ImageView is
                // just upscaling a low-resolution bitmap.
                val viewWidth = pageView.width
                val renderScale = RENDER_SCALE
                val bitmapWidth = (viewWidth * renderScale).toInt().coerceAtLeast(viewWidth)
                val bitmapHeight = calculateBitmapHeight(bitmapWidth, position)

                // View layout (matches the device width — keep this UNCHANGED
                // so the row occupies the right vertical space in the list).
                val itemHeight = calculateBitmapHeight(itemBinding.root.width, position)
                val layoutParams = itemBinding.root.layoutParams as ViewGroup.MarginLayoutParams
                Log.i("Item height","$bitmapWidth-$bitmapHeight-$itemHeight-${layoutParams.height}")

                layoutParams.height = itemHeight
                layoutParams.setMargins(
                    pageSpacing.left,
                    pageSpacing.top,
                    pageSpacing.right,
                    pageSpacing.bottom
                )
                itemBinding.root.layoutParams = layoutParams
                Log.d("PdfViewAdapter", "BEFORE    Bitmap Width: $bitmapWidth, Device Width: ${context.resources.displayMetrics.widthPixels}")

                // Reuse a bitmap at the higher resolution.
                val bitmap = CommonUtils.Companion.BitmapPool.getBitmap(bitmapWidth, bitmapHeight)

                renderer.renderPage(position, bitmap) { success, pageNo, renderedBitmap ->
                    if (success && pageNo == position) {
                        CoroutineScope(Dispatchers.Main).launch {
                            // FIT_CENTER scales the (now-higher-res) bitmap
                            // down to fit the view — pinch-zoom then reveals
                            // the extra pixels rather than upscaling.
                            itemBinding.pageView.scaleType = ImageView.ScaleType.FIT_CENTER
                            renderedBitmap?.let {
                                Log.d("PdfViewAdapter", "renderedBitmap Width: ${it.width}, Bitmap Height: ${it.height}")
                            }
                            bitmap?.let {
                                Log.d("PdfViewAdapter", "Bitmap Width: ${it.width}, Bitmap Height: ${it.height}")
                            }

                            itemBinding.pageView.apply {
                                setImageBitmap(renderedBitmap ?: bitmap)
                            }
                            applyFadeInAnimation(pageView)
                            pageLoadingLayout.pdfViewPageLoadingProgress.hide()
                        }
                        // Prefetch pages after rendering the current page
                        renderer.prefetchPages(position, bitmapWidth, bitmapHeight)
                    } else {
                        CommonUtils.Companion.BitmapPool.recycleBitmap(bitmap)
                    }
                }

                bindAnnotationOverlay(position)
            }
        }

        fun bindOverlayOnly(position: Int) {
            bindAnnotationOverlay(position)
        }

        private fun bindAnnotationOverlay(position: Int) {
            with(itemBinding.annotationOverlay) {
                pageIndex = position
                annotations = this@PdfViewAdapter.annotations
                annotationMode = this@PdfViewAdapter.annotationMode
                listener = annotationListener
                highlightColor = this@PdfViewAdapter.highlightColor
                highlightWidthMultiplier = this@PdfViewAdapter.highlightWidthMultiplier
                highlightHeightMultiplier = this@PdfViewAdapter.highlightHeightMultiplier
                stickyNoteColor = this@PdfViewAdapter.stickyNoteColor
                stickyNoteSizeMultiplier = this@PdfViewAdapter.stickyNoteSizeMultiplier
                longPressListener = this@PdfViewAdapter.longPressListener
                offModeTapListener =
                    PdfAnnotationOverlayView.OffModeTapListener { ann ->
                        annotationTapListener?.onAnnotationTapped(ann)
                    }
            }
        }

        private fun calculateBitmapHeight(width: Int, position: Int): Int {
            // Get the actual dimensions of the PDF page
            val pageDimensions = renderer.getPageDimensions(position)
            // Calculate the aspect ratio of the PDF page
            val aspectRatio = pageDimensions.width.toFloat() / pageDimensions.height.toFloat()
            // Calculate the height based on the width of the ImageView and the aspect ratio
            return (width / aspectRatio).toInt()
        }

        private fun applyFadeInAnimation(view: View) {
            view.animation = AlphaAnimation(0F, 1F).apply {
                interpolator = LinearInterpolator()
                duration = 300
                start()
            }
        }

        private fun handleLoadingForPage(position: Int) {
            with(itemBinding) {
                if (!enableLoadingForPages || renderer.pageExistInCache(position)) {
                    pageLoadingLayout.pdfViewPageLoadingProgress.hide()
                } else {
                    pageLoadingLayout.pdfViewPageLoadingProgress.show()
                }
            }
        }
    }

    companion object {
        /**
         * 2025-08-20 (audit-fix rev 8): render the PDF page at 1.5× the view
         * width so that pinch-zoom (up to 5×) reveals pixel detail rather
         * than upscaling a low-resolution bitmap.
         *
         * The previous value was 1.0f despite the comment claiming 2× —
         * the comment and the constant disagreed, so the user got blurry
         * text on zoom. 1.5× is a pragmatic trade-off: ~2.25× the byte
         * count of 1.0×, but pinching to 2× still looks crisp on a 400dpi
         * phone screen. 2.0× would be even crisper but risks OOM on
         * low-RAM devices with large PDFs.
         *
         * The BitmapPool + PdfRendererCore's own disk cache keep memory
         * bounded — only visible + prefetched pages hold bitmaps.
         *
         * Set to 1.0f to restore the legacy (blurry on zoom) behaviour.
         */
        private const val RENDER_SCALE: Float = 1.5f

        /** 2025-08-19 (audit rev6): payload key — update only overlay, not bitmap. */
        const val PAYLOAD_OVERLAY_ONLY: String = "overlay_only"
    }
}
