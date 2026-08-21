package com.horizontal.pdfviewer.annotations

import android.content.Context
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.Log
import com.horizontal.pdfviewer.PdfRendererView
import java.util.Locale

/**
 * Text-to-Speech controller for the PDF reader
 * (audit-2025-08-19 / Feature 1.C).
 *
 * Design:
 * - Uses Android's built-in [TextToSpeech] engine — zero new dependencies.
 * - Honours a [TextProvider] hook the host installs. The default provider
 *   is a NO-OP that emits a placeholder sentence ("Page N of M, no text
 *   extractor wired up") so the controller is fully testable without a
 *   third-party PDF text extraction library.
 * - Real PDF text extraction is intentionally left as a hook because the
 *   Android framework's [android.graphics.pdf.PdfRenderer] only renders
 *   pages to bitmaps (no text API). To read actual page text the developer
 *   can either:
 *     1. Add `com.tom-roush:pdfbox-android` (or `com.itextpdf:itextpdf`)
 *        and implement [TextProvider] with one of those libraries.
 *     2. Run an on-device OCR pass (Tesseract / ML Kit) over the rendered
 *        bitmaps — the existing `PdfRendererCore` already produces them.
 *   The hook signature is stable across both options so the audit document
 *   can recommend either.
 *
 * Lifecycle: the controller owns the [TextToSpeech] instance and releases
 * it in [shutdown]. The host activity should call `shutdown()` from
 * `onDestroy` (and optionally `stop()` from `onPause`).
 *
 * ===== Merge notes for the developer =====
 *
 * Wiring inside [org.horizontal.tella.mobile.views.activity.viewer.PDFReaderActivity]:
 *
 *     private lateinit var tts: PdfTtsController
 *
 *     // in onCreate after binding.pdfRendererView is initialised:
 *     tts = PdfTtsController(this, binding.pdfRendererView).also {
 *         it.textProvider = PdfTtsController.PageTextProvider { page ->
 *             MyPdfBoxExtractor.extract(context, fileDescriptor, page) // ← your impl
 *         }
 *     }
 *
 *     // onOptionsItemSelected:
 *     R.id.menu_item_pdf_tts_play -> { tts.playFromCurrentPage(); true }
 *     R.id.menu_item_pdf_tts_stop -> { tts.stop(); true }
 *
 *     // onDestroy:
 *     tts.shutdown()
 *
 * The corresponding menu items live in `pdf_annotation_menu.xml`
 * (already added — see `menu_item_pdf_tts_play` / `menu_item_pdf_tts_stop`).
 */
class PdfTtsController(
    private val context: Context,
    private val pdfView: PdfRendererView
) {

    /** Hook the host installs to provide page text. Default = placeholder. */
    fun interface PageTextProvider {
        fun textFor(pageIndex: Int): String
    }

    var textProvider: PageTextProvider = PageTextProvider { page ->
        // Default placeholder so the controller is testable in isolation.
        "Page ${(page + 1)}. No text extractor wired up; install a " +
            "PageTextProvider to read the page aloud."
    }

    private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false
    private var utteranceIdCounter: Int = 0

    /** Whether the engine is currently speaking. */
    val isSpeaking: Boolean
        get() = tts?.isSpeaking == true

    /**
     * Initialises the TTS engine asynchronously. Safe to call multiple
     * times — subsequent calls are no-ops once the engine is ready.
     */
    fun ensureInitialised() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.getDefault())
                ready = res != TextToSpeech.LANG_MISSING_DATA &&
                        res != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ready) {
                    Log.w(TAG, "Default language not available; falling back to en-US")
                    tts?.setLanguage(Locale.US)
                    ready = true
                }
                // 2025-08-19 (audit-fix): install the utterance-progress
                // listener ONCE during init, not on every playFromCurrentPage
                // call. The previous version re-installed the listener on
                // every play, which:
                //  (a) leaked the previous lambda + its captured `pageToRead`
                //      local (small but unbounded growth across many calls),
                //  (b) caused the auto-advance to start from the OLDEST
                //      captured pageToRead if the user paused and resumed
                //      multiple times (because the listener's `lastReadPage`
                //      shadowed the field and wasn't updated between
                //      listener re-installs).
                // The listener now reads from the `lastReadPage` field
                // directly (no closure capture), so re-installs are not
                // needed.
                installUtteranceProgressListener()
            } else {
                Log.e(TAG, "TTS init failed with status=$status")
            }
        }
    }

    /**
     * 2025-08-19 (audit-fix): factored out of [playFromCurrentPage] so we
     * install the listener exactly once per TTS instance. The listener
     * advances through the document via the [lastReadPage] field rather
     * than a captured local, so subsequent playFromCurrentPage calls just
     * update the field and call [speakPage] — no listener re-install.
     */
    private fun installUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object :
            android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                // Read the next page if we haven't passed the end.
                val next = lastReadPage + 1
                if (next < pdfView.totalPageCount) {
                    lastReadPage = next
                    pdfView.scrollToPage(next)
                    speakPage(next)
                }
            }
            override fun onError(utteranceId: String?) {}
        })
    }

    /**
     * Reads the text of [page] (zero-based). Does NOT advance through the
     * document — see [playFromCurrentPage] for that.
     */
    fun speakPage(page: Int) {
        ensureInitialised()
        val safe = page.coerceAtLeast(0)
        val text = textProvider.textFor(safe).take(MAX_CHARS)
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, nextUtteranceId())
    }

    /**
     * Starts reading from the page the user is currently on. The TTS
     * engine calls the utterance-progress listener (installed in
     * [ensureInitialised]) when each utterance finishes — the listener
     * advances to the next page automatically so the user can sit back
     * and listen.
     *
     * 2025-08-19 (audit-fix): the previous version re-installed the
     * listener here on every call, which leaked lambdas and could
     * resume from a stale page. The listener is now installed once
     * during init.
     */
    fun playFromCurrentPage() {
        ensureInitialised()
        if (!ready) {
            Log.w(TAG, "TTS not ready yet — please try again in a moment")
            return
        }
        val pageToRead = pdfView.currentPageIndex
        lastReadPage = pageToRead
        speakPage(pageToRead)
    }

    /** Stops any in-progress speech. Safe to call when nothing is playing. */
    fun stop() {
        tts?.stop()
    }

    /** Releases native TTS resources. Call from host `onDestroy`. */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
        ready = false
    }

    private var lastReadPage: Int = 0

    private fun nextUtteranceId(): String =
        "tella-pdf-tts-${utteranceIdCounter++}"

    companion object {
        private const val TAG = "PdfTtsController"
        // Android TTS has a ~4000 char soft limit per utterance on some
        // engines; cap well below that to be safe across devices.
        private const val MAX_CHARS = 3500
    }
}
