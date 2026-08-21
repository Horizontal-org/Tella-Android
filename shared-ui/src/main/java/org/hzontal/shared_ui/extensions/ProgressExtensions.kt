package org.hzontal.shared_ui.extensions

import android.os.Build
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator

fun CircularProgressIndicator.setProgressPercent(progress: Int, animation: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.setProgress(progress, animation)
    } else {
        this.progress = progress
    }
}

fun LinearProgressIndicator.setProgressPercent(progress: Int, animation: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.setProgress(progress, animation)
    } else {
        this.progress = progress
    }

}