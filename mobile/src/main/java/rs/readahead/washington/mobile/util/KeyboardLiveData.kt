package rs.readahead.washington.mobile.util

import android.graphics.Rect
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.MutableLiveData

class KeyboardLiveData(private val viewGroup: ViewGroup) : MutableLiveData<Pair<Boolean, Double>>() {

    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var instance: KeyboardLiveData? = null


   fun init(viewGroup: ViewGroup): KeyboardLiveData? {
            if (instance == null)
                instance = KeyboardLiveData(viewGroup)
            return instance
        }

    fun status(): KeyboardLiveData? {
        if (instance == null)
            throw IllegalAccessException("Call init with activity reference before accessing status")
        return instance
    }

    private fun addOnGlobalLayoutListener() {

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

            val r = Rect()
            viewGroup.getWindowVisibleDisplayFrame(r)
            val screenHeaight = viewGroup.rootView.height
            val keypadHeight = screenHeaight - r.bottom
            if (keypadHeight > screenHeaight * 0.15) {
                //keyboard Open
                postValue(
                    Pair(
                        true,
                        (keypadHeight.toDouble() / (screenHeaight * 1.1))
                    )
                )
            } else {
                //keyboard closed
                postValue(Pair(false, 0.0))

            }
        }
        viewGroup.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

    }

    private fun removeOnGlobalLayoutListener() {
        viewGroup.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
        globalLayoutListener = null
    }

    override fun onActive() {
        super.onActive()
        addOnGlobalLayoutListener()
    }

    override fun onInactive() {
        super.onInactive()
        removeOnGlobalLayoutListener()
    }


}
