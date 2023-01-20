package rs.readahead.washington.mobile.views.custom

import android.content.Context
import android.graphics.Canvas
import android.location.Criteria
import android.location.Location
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class MapViewOverlay (context: Context?, attrs: AttributeSet?) :
    MapView(context, attrs) {
    private var tapOverlay: Overlay? = null
    private var onTapListener: OnTapListener? = null

    private fun prepareTagOverlay() {
        tapOverlay = object : Overlay(this.context) {
            override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {}
            override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
                val proj = mapView.projection
                val geoPoint = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                if (onTapListener != null) {
                    val location = Location("")
                    location.latitude = geoPoint.latitudeE6.toDouble() / 1000000
                    location.longitude = geoPoint.longitudeE6.toDouble() / 1000000
                    location.accuracy = Criteria.ACCURACY_FINE.toFloat()
                    onTapListener!!.onMapLongPress(location)
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val proj = mapView.projection
                val geoPoint = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                if (onTapListener != null) {
                    onTapListener!!.onMapTapped(geoPoint)
                }
                return true
            }
        }
    }

    fun addTapListener(onTapListener: OnTapListener?) {
        prepareTagOverlay()
        this.overlays.add(0, tapOverlay)
        this.onTapListener = onTapListener
    }

    fun removeTapListener() {
        if (tapOverlay != null && this.overlays.size > 0) {
            this.overlays.removeAt(0)
        }
        tapOverlay = null
        onTapListener = null
    }

    interface OnTapListener {
        fun onMapLongPress(location: Location?)
        fun onMapTapped(geoPoint: GeoPoint?)
    }
}