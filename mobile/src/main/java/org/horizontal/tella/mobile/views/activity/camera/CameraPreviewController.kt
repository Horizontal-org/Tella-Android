package org.horizontal.tella.mobile.views.activity.camera

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.SeekBar
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class CameraPreviewController(
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val focusIndicator: View,
    private val zoomSlider: SeekBar,
    private val cameraProvider: () -> Camera?,
    private val isCameraActive: () -> Boolean,
) {
    var zoomLevel = 0
        private set

    private val mainExecutor = ContextCompat.getMainExecutor(previewView.context)
    private var focusRequestId = 0L
    private var isTapFocusActive = false
    private val resetFocusRunnable = Runnable { resetFocus() }

    fun attach() {
        setupZoomSlider()
        setupPreviewGestures()
        previewView.previewStreamState.observe(lifecycleOwner) { state ->
            if (state != PreviewView.StreamState.STREAMING) {
                resetFocus()
            }
        }
    }

    fun syncZoomSlider() {
        zoomSlider.progress = zoomLevel
    }

    fun applyZoom() {
        resetFocus()
        cameraProvider()?.cameraControl?.setLinearZoom(zoomLevel.toFloat() / 100f)
    }

    fun resetZoom() {
        zoomLevel = 0
        zoomSlider.progress = 0
        applyZoom()
    }

    fun resetFocus() {
        clearFocusIndicator()
        if (!isTapFocusActive) return
        isTapFocusActive = false
        cameraProvider()?.cameraControl?.cancelFocusAndMetering()
    }

    private fun setupZoomSlider() {
        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                zoomLevel = progress
                if (fromUser) applyZoom()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPreviewGestures() {
        val context = previewView.context
        var hadMultiplePointers = false
        val tapDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    if (!hadMultiplePointers) {
                        focusAt(event.x, event.y)
                    }
                    return true
                }
            })
        tapDetector.setOnDoubleTapListener(null)
        val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    resetFocus()
                    return cameraProvider() != null
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    pinchZoom(detector.scaleFactor)
                    return true
                }
            })
        previewView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> hadMultiplePointers = false
                MotionEvent.ACTION_POINTER_DOWN -> hadMultiplePointers = true
            }
            scaleDetector.onTouchEvent(event)
            tapDetector.onTouchEvent(event)
            true
        }
    }

    private fun pinchZoom(scaleFactor: Float) {
        val camera = cameraProvider() ?: return
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val newRatio = (zoomState.zoomRatio * scaleFactor)
            .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        if (newRatio == zoomState.zoomRatio) return
        val future = camera.cameraControl.setZoomRatio(newRatio)
        future.addListener({
            if (cameraProvider() !== camera) return@addListener
            val linearZoom = camera.cameraInfo.zoomState.value?.linearZoom ?: return@addListener
            zoomSlider.progress = (linearZoom * 100f).toInt()
        }, mainExecutor)
    }

    private fun focusAt(focusX: Float, focusY: Float) {
        val camera = cameraProvider() ?: return
        if (!isCameraActive() ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
            previewView.previewStreamState.value != PreviewView.StreamState.STREAMING ||
            focusX < 0f || focusX >= previewView.width || focusY < 0f || focusY >= previewView.height
        ) return

        val point = previewView.meteringPointFactory.createPoint(focusX, focusY)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(TAP_FOCUS_DURATION_MS, TimeUnit.MILLISECONDS)
            .build()
        if (!camera.cameraInfo.isFocusMeteringSupported(action)) return

        clearFocusIndicator()
        val requestId = focusRequestId
        isTapFocusActive = true
        focusIndicator.apply {
            x = previewView.left + focusX - width / 2f
            y = previewView.top + focusY - height / 2f
            visibility = View.VISIBLE
            postDelayed(resetFocusRunnable, TAP_FOCUS_DURATION_MS)
        }
        val focusFuture = camera.cameraControl.startFocusAndMetering(action)
        focusFuture.addListener({
            if (requestId != focusRequestId) return@addListener
            try {
                if (!focusFuture.get().isFocusSuccessful) {
                    resetFocus()
                }
            } catch (_: CancellationException) {
                resetFocus()
            } catch (e: ExecutionException) {
                resetFocus()
                Timber.d(e, "Unable to focus camera")
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                resetFocus()
            }
        }, mainExecutor)
    }

    private fun clearFocusIndicator() {
        focusRequestId++
        focusIndicator.removeCallbacks(resetFocusRunnable)
        focusIndicator.visibility = View.INVISIBLE
    }

    private companion object {
        const val TAP_FOCUS_DURATION_MS = 3_000L
    }
}
