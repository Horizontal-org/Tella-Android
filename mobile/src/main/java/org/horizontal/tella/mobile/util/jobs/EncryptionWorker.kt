package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class EncryptionWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Retrieve file data from input
        val fileData = inputData.getString("file")
        // Perform encryption logic here
        // ...
        // Return a success or failure result
        return Result.success()
    }
}