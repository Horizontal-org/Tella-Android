package com.horizontal.pdfviewer.annotations

import android.content.Context
import android.graphics.Color
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import java.io.File
import java.io.InputStream

/**
 * 2025-08-20 (audit-fix rev 8): Flatten PDF annotations into a new PDF file.
 *
 * ## What this does
 *
 * Takes the original PDF (as a stream) + the list of annotations stored in
 * [PdfAnnotationStore] and produces a NEW PDF file with the annotations
 * "baked in" — highlights are drawn as semi-transparent rectangles on the
 * page, and sticky notes are drawn as colored circles with their text
 * rendered next to them.
 *
 * The output is a standalone PDF that can be shared with anyone — they
 * don't need Tella to see the annotations.
 *
 * ## Why PDFBox (not Android's PdfRenderer)
 *
 * Android's framework `PdfRenderer` can only RENDER pages to bitmaps — it
 * cannot write new content into a PDF. PDFBox-Android supports both
 * reading and writing PDFs, including adding content streams to existing
 * pages. We already depend on PDFBox for [PdfTextExtractor], so this is
 * a free win.
 *
 * ## Coordinate system
 *
 * PDFBox's coordinate system has the origin at the BOTTOM-left corner of
 * the page (Y increases upward). Our [PdfAnnotation] stores coordinates as
 * page-relative fractions (0..1) with the origin at the TOP-left (Y
 * increases downward — standard Android convention). So when drawing we
 * must flip Y: `pdfY = pageHeight * (1 - ann.y - ann.height)`.
 *
 * ## Sticky note text rendering
 *
 * PDFBox needs a font to render text. We load the built-in Helvetica font
 * once per document (it's bundled in the PDFBox AAR). For sticky notes
 * with non-ASCII text (e.g. CJK), Helvetica will fall back to drawing
 * tofu boxes — that's a known limitation. A future version could embed
 * a Unicode TTF, but that adds ~5 MB to the output PDF.
 *
 * ## Memory
 *
 * PDDocument loads the entire PDF into memory. For a 50 MB PDF this is
 * roughly 100 MB of heap. We load + save + close in a single `use {}`
 * block so the memory is released as soon as the flattened file is
 * written. The output file is written to the app's cache dir and
 * returned as a [File].
 *
 * ## Thread safety
 *
 * NOT thread-safe — the caller must ensure only one flatten operation
 * runs at a time. [PDFReaderActivity.sharePdfWithAnnotations] runs this
 * on a single IO coroutine, so that's fine.
 */
object PdfAnnotationFlattener {

    /**
     * Flattens the given [inputStream] PDF + [annotations] into a new PDF
     * file written to the app's cache dir.
     *
     * @param context used to locate the cache dir and initialise PDFBox resources.
     * @param inputStream the original PDF stream (will be spooled to a tmp file).
     * @param annotations the annotations to bake in.
     * @param outputFileName the name of the output PDF file (e.g. "document_annotated.pdf").
     * @return the output [File], or null if flattening failed.
     */
    fun flatten(
        context: Context,
        inputStream: InputStream,
        annotations: List<PdfAnnotation>,
        outputFileName: String
    ): File? {
        if (annotations.isEmpty()) return null
        ensureInitialised(context)

        val tmpInput = File.createTempFile("flatten_in_", ".pdf", context.cacheDir)
        val tmpOutput = File(context.cacheDir, outputFileName)
        return try {
            // Spool the input stream to a tmp file (PDFBox needs a seekable File).
            inputStream.use { input ->
                tmpInput.outputStream().use { out -> input.copyTo(out) }
            }

            PDDocument.load(tmpInput).use { doc ->
                // 2025-08-20 (audit-fix rev 9): use PDFBox's built-in standard
                // 14 font (Helvetica) via PDType1Font.HELVETICA. The previous
                // code tried to load "fonts/Helvetica.ttf" from assets, which
                // doesn't exist in this project — the catch block silently
                // returned null, so sticky-note text never rendered. Using
                // PDType1Font.HELVETICA requires no asset file and is always
                // available because it's one of the 14 standard PDF fonts.
                val font = try {
                    com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA
                } catch (_: Throwable) {
                    null
                }

                // Group annotations by page so we only open one content
                // stream per page (opening a content stream per annotation
                // would re-init the graphics state and is slower).
                val byPage = annotations.groupBy { it.page }

                byPage.forEach { (pageIndex, pageAnns) ->
                    if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return@forEach
                    val page: PDPage = doc.getPage(pageIndex)
                    val pageRect: PDRectangle = page.mediaBox
                    val pageW = pageRect.width
                    val pageH = pageRect.height

                    // 2026-08-20 (audit-fix rev 11): use the 5-parameter
                    // constructor with resetContext=true. The page's existing
                    // content stream may have a modified CTM (current
                    // transformation matrix) that shifts/scales the
                    // coordinate system. Without resetContext, our
                    // coordinates are interpreted in that shifted system,
                    // causing highlights to appear at the wrong position.
                    // resetContext=true inserts a `q` (save) + `cm` (reset
                    // identity matrix) at the start of our content stream,
                    // so our coordinates are in the page's default
                    // coordinate system.
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        pageAnns.forEach { ann ->
                            when (ann.type) {
                                PdfAnnotation.Type.HIGHLIGHT -> drawHighlight(cs, ann, pageW, pageH)
                                PdfAnnotation.Type.STICKY_NOTE -> drawStickyNote(cs, ann, pageW, pageH, font)
                            }
                        }
                    }
                }

                doc.save(tmpOutput)
            }
            tmpOutput
        } catch (t: Throwable) {
            // Best-effort cleanup of a half-written output.
            try { if (tmpOutput.exists()) tmpOutput.delete() } catch (_: Throwable) {}
            null
        } finally {
            try { tmpInput.delete() } catch (_: Throwable) {}
        }
    }

    /**
     * Draws a highlight annotation as a semi-transparent rectangle.
     *
     * The annotation's [PdfAnnotation.color] is an ARGB int; we extract the
     * RGB channels and apply the alpha via [PDPageContentStream.setNonStrokingColor]
     * + a separate alpha state. PDFBox's `setNonStrokingColor(int, int, int)`
     * takes 0-255 RGB values.
     *
     * The highlight height is the annotation's stored height (which was
     * computed by [PdfAnnotationOverlayView.commitHighlightAt] as
     * `lineH / pageHeight` where `lineH = 24dp * density * heightMultiplier`).
     */
    private fun drawHighlight(
        cs: PDPageContentStream,
        ann: PdfAnnotation,
        pageW: Float,
        pageH: Float
    ) {
        val x = ann.x * pageW
        val w = ann.width * pageW
        // Y flip: PDF origin is bottom-left, our annotation origin is top-left.
        val y = (1f - ann.y - ann.height) * pageH
        val h = ann.height * pageH

        val r = Color.red(ann.color) / 255f
        val g = Color.green(ann.color) / 255f
        val b = Color.blue(ann.color) / 255f

        try {
            // 2026-08-20 (audit-fix rev 10): use PDExtendedGraphicsState to
            // make the highlight SEMI-TRANSPARENT. The previous code just
            // called cs.fill() which fills at 100% opacity — the highlight
            // covered the text beneath it completely. The user reported
            // "why it not make highlight transparent this is a big issue".
            //
            // PDFBox uses the PDF graphics state model: you push a new
            // graphics state with setGraphicsStateParameters, set the
            // non-stroking alpha (fill opacity) to 0.35 (35% — matches the
            // on-screen HIGHLIGHT_ALPHA = 90/255 ≈ 35%), draw the rect,
            // then pop the state via restoreGraphicsState so subsequent
            // draws on the same page are not affected.
            cs.saveGraphicsState()
            val gs = PDExtendedGraphicsState()
            // 0.35 = 35% opacity — matches PdfAnnotationOverlayView.HIGHLIGHT_ALPHA
            gs.setNonStrokingAlphaConstant(0.35f)
            gs.setStrokingAlphaConstant(0.35f)
            cs.setGraphicsStateParameters(gs)
            cs.setNonStrokingColor(r, g, b)
            cs.addRect(x, y, w, h)
            cs.fill()
            cs.restoreGraphicsState()
        } catch (_: Throwable) { /* best-effort */ }
    }

    /**
     * Draws a sticky note as a colored circle with the text rendered next to it.
     *
     * The circle is drawn at the annotation's (x, y) position. The text is
     * rendered to the right of the circle, wrapping at ~page width * 0.3.
     */
    private fun drawStickyNote(
        cs: PDPageContentStream,
        ann: PdfAnnotation,
        pageW: Float,
        pageH: Float,
        font: com.tom_roush.pdfbox.pdmodel.font.PDFont?
    ) {
        val cx = ann.x * pageW
        val cy = (1f - ann.y) * pageH  // flip Y
        val r = (ann.width * pageW).coerceAtLeast(8f) / 2f

        val red = Color.red(ann.color) / 255f
        val green = Color.green(ann.color) / 255f
        val blue = Color.blue(ann.color) / 255f

        try {
            // Draw the pushpin circle.
            cs.setNonStrokingColor(red, green, blue)
            // PDFBox-Android 2.0.27 doesn't have a direct drawCircle method.
            // Approximate with 4 cubic Bézier arcs using curveTo (the
            // PDFBox-Android name for the Bézier-to command; the desktop
            // PDFBox calls it curveTo too — `addBezier` was a wrong guess).
            val kappa = 0.5522848f
            cs.moveTo(cx + r, cy)
            cs.curveTo(cx + r, cy + r * kappa, cx + r * kappa, cy + r, cx, cy + r)
            cs.curveTo(cx - r * kappa, cy + r, cx - r, cy + r * kappa, cx - r, cy)
            cs.curveTo(cx - r, cy - r * kappa, cx - r * kappa, cy - r, cx, cy - r)
            cs.curveTo(cx + r * kappa, cy - r, cx + r, cy - r * kappa, cx + r, cy)
            cs.fill()

            // Draw the text next to the circle (if we have a font and text).
            if (font != null && ann.text.isNotBlank()) {
                val fontSize = 10f
                cs.beginText()
                cs.setFont(font, fontSize)
                cs.setNonStrokingColor(0f, 0f, 0f)  // black text
                // Position the text just to the right of the circle.
                val textX = cx + r + 4f
                val textY = cy - fontSize / 3
                cs.newLineAtOffset(textX, textY)
                // Wrap the text at ~30% of page width.
                val maxWidth = pageW * 0.3f
                val wrapped = wrapText(ann.text, font, fontSize, maxWidth)
                cs.showText(wrapped.first())
                wrapped.drop(1).forEach { line ->
                    cs.newLineAtOffset(0f, -fontSize * 1.2f)
                    cs.showText(line)
                }
                cs.endText()
            }
        } catch (_: Throwable) { /* best-effort */ }
    }

    /**
     * Naive text wrapper — splits on word boundaries until the accumulated
     * width exceeds [maxWidth]. Doesn't handle CJK well (CJK has no word
     * boundaries), but it's good enough for sticky notes which are usually
     * short.
     */
    private fun wrapText(text: String, font: com.tom_roush.pdfbox.pdmodel.font.PDFont, fontSize: Float, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            try {
                val width = font.getStringWidth(candidate) / 1000 * fontSize
                if (width > maxWidth && current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                } else {
                    current = StringBuilder(candidate)
                }
            } catch (_: Throwable) {
                // getStringWidth can throw for unsupported chars; just add
                // the word without measuring.
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }

    @Volatile
    private var initialised: Boolean = false
    private val initLock = Any()

    private fun ensureInitialised(context: Context) {
        if (initialised) return
        synchronized(initLock) {
            if (initialised) return
            try {
                PDFBoxResourceLoader.init(context)
                initialised = true
            } catch (_: Throwable) {
                initialised = false
            }
        }
    }
}
