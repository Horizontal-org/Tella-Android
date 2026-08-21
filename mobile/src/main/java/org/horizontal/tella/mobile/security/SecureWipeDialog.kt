package org.horizontal.tella.mobile.security

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.crash.CrashReporterProvider
import org.horizontal.tella.mobile.views.activity.viewer.TellaDialogs

/**
 * Secure Wipe prompt + progress dialog. Audit / Feature 2 (2025-08-19).
 *
 * Shows the user a yes/no bottom-sheet-style prompt explaining what secure
 * wipe does, then if accepted runs [SecureWipeManager.wipe] on a
 * background Single and surfaces a tiny progress dialog. Result is
 * delivered to [onResult].
 *
 * 2025-08-20 (audit-fix rev 7): both AlertDialogs (the prompt AND the
 * progress dialog) now go through [TellaDialogs.builder] so the
 * "Secure wipe" / "Skip" buttons on the prompt are visible (was inherited
 * wa_white_80 = invisible white-on-white). The progress dialog has no
 * buttons so it isn't affected by the bug, but we route it through
 * TellaDialogs.builder anyway for consistency.
 */
object SecureWipeDialog {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Holds the disposable for a wipe that may or may not have started.
     *
     * 2025-08-19 (audit-fix): The previous `promptAndWipe` API claimed to
     * return a `Disposable?` but always returned `null` — the disposable
     * is only created inside the click handler (after the user accepts
     * the prompt), so it cannot be returned synchronously.
     *
     * This holder is returned synchronously; the caller registers an
     * `onDispose` callback (or polls `disposable` from `onDestroy`) and
     * can dispose the background job whenever the host view is destroyed.
     */
    class WipeHandle {
        @Volatile var disposable: Disposable? = null
            internal set

        /** Convenience: dispose whatever is currently held (no-op if null). */
        fun dispose() {
            disposable?.dispose()
        }
    }

    /**
     * Shows the prompt; on accept runs the wipe in the background.
     *
     * Returns a [WipeHandle] synchronously so the caller can dispose the
     * background job from `onDestroyView` / `onDestroy` even if the user
     * accepts the prompt late. If the user declines, [onResult] is
     * invoked with `false` and the handle's disposable stays null.
     */
    fun promptAndWipe(
        context: Context,
        uri: Uri,
        onResult: (Boolean) -> Unit
    ): WipeHandle {
        val handle = WipeHandle()
        // 2025-08-20 (audit-fix rev 7): TellaDialogs.builder applies the
        // TellaDialogTheme overlay so the "Secure wipe" / "Skip" buttons
        // are visible (was: plain AlertDialog.Builder(context) which
        // inherited colorAccent = wa_white_80 = invisible).
        TellaDialogs.builder(context)
            .setTitle(R.string.secure_wipe_title)
            .setMessage(context.getString(R.string.secure_wipe_message))
            .setPositiveButton(R.string.secure_wipe_action) { d, _ ->
                d.dismiss()
                handle.disposable = runWipe(context, uri, onResult)
            }
            .setNegativeButton(R.string.secure_wipe_skip) { d, _ ->
                d.dismiss()
                onResult(false)
            }
            .show()
        return handle
    }

    /**
     * Performs the wipe and surfaces progress. Skips the prompt — useful
     * for callers that have already confirmed (e.g. via a settings switch).
     */
    fun runWipe(
        context: Context,
        uri: Uri,
        onResult: (Boolean) -> Unit
    ): Disposable {
        // Inflated lazily so the caller doesn't pay the inflation cost
        // unless the user accepts the prompt.
        val progressView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_secure_wipe, null)
        val progressBar = progressView.findViewById<ProgressBar>(R.id.secureWipeProgress)
        val progressText = progressView.findViewById<TextView>(R.id.secureWipeProgressText)
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        progressText.text = context.getString(R.string.secure_wipe_progress, 0)
        progressText.visibility = View.VISIBLE

        val dialog = TellaDialogs.builder(context)
            .setTitle(R.string.secure_wipe_title)
            .setView(progressView)
            .setCancelable(false)
            .show()

        val wiper = SecureWipeManager(context.applicationContext)
        return Single.fromCallable {
            wiper.wipe(uri) { pct ->
                // Called from the IO thread — marshal to main.
                mainHandler.post {
                    progressBar.progress = pct
                    progressText.text = context.getString(R.string.secure_wipe_progress, pct)
                }
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ ok ->
                dialog.dismiss()
                onResult(ok)
            }, { t ->
                CrashReporterProvider.get()
                    .recordException(t)
                dialog.dismiss()
                onResult(false)
            })
    }
}

