package rs.readahead.washington.mobile.data.repository

import androidx.lifecycle.MutableLiveData
import rs.readahead.washington.mobile.domain.entity.IProgressListener
import rs.readahead.washington.mobile.util.Util

class ProgressListener(private val partName: String,private val progressCallBack : MutableLiveData<Pair<String, Float>>?) : IProgressListener {

    private var time: Long = 0

    override fun onProgressUpdate(current: Long, total: Long) {
        val now = Util.currentTimestamp()
        if (progressCallBack != null && now - time > REFRESH_TIME_MS) {
            time = now
            progressCallBack.postValue(Pair(partName,(current/total).toFloat()))
        }
    }

    companion object {
        private const val REFRESH_TIME_MS: Long = 500
    }
}