package org.horizontal.tella.mobile.data.repository

import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.IProgressListener
import org.horizontal.tella.mobile.util.Util

class ProgressListener(private val partName: String,private val progressCallBack : SingleLiveEvent<Pair<String, Float>>?) : IProgressListener {

    private var time: Long = 0

    override fun onProgressUpdate(current: Long, total: Long) {
        val now = Util.currentTimestamp()
        if (progressCallBack != null && now - time > REFRESH_TIME_MS) {
            time = now
            progressCallBack.postValue(Pair(partName,current.toFloat()/total.toFloat()))
        }
    }

    companion object {
        private const val REFRESH_TIME_MS: Long = 500
    }
}