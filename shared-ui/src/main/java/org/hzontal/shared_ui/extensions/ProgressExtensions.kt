package org.hzontal.shared_ui.bottomsheet

import android.os.Build
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator

fun CircularProgressIndicator.setProgressImport(progress: Int, animation: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.setProgress(progress, animation)
    } else {
        this.progress = progress
    }
}

fun LinearProgressIndicator.setProgressImport(progress: Int, animation: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.setProgress(progress, animation)
    } else {
        this.progress = progress
    }

}