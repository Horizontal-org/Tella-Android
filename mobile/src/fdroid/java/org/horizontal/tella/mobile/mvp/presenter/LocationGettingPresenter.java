package org.horizontal.tella.mobile.mvp.presenter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import java.lang.ref.WeakReference;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.mvp.contract.ILocationGettingPresenterContract;
import org.horizontal.tella.mobile.util.LocationUtil;

/**
 * F-Droid implementation: uses only Android framework LocationManager (no Google Play Services).
 */
public class LocationGettingPresenter implements ILocationGettingPresenterContract.IPresenter {
    private static final long LOCATION_REQUEST_INTERVAL_MS = 100;
    private static final float LOCATION_MIN_DISTANCE_M = 0f;

    private ILocationGettingPresenterContract.IView view;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean listenerRegistered;
    private boolean untilThreshold;
    private float threshold;
    private Location currentBestLocation;

    public LocationGettingPresenter(ILocationGettingPresenterContract.IView view, boolean untilThreshold) {
        this.view = view;
        this.untilThreshold = untilThreshold;
        threshold = Preferences.getLocationAccuracyThreshold();
        locationManager = (LocationManager) view.getContext().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener(this);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void startGettingLocation(boolean useLastKnownLocation) {
        if (noLocationPermissions()) {
            view.onNoLocationPermissions();
            return;
        }

        if (!isGPSProviderEnabled()) {
            view.onGPSProviderDisabled();
            return;
        }

        view.onGettingLocationStart();
        currentBestLocation = null;

        if (useLastKnownLocation && locationManager != null) {
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) {
                sendLocation(last);
            }
        }

        startLocationListening();
    }

    @Override
    public void stopGettingLocation() {
        stopLocationListening();

        if (view != null) {
            view.onGettingLocationEnd();
        }
    }

    @Override
    public boolean isLocationPermissionAllowed() {
        return !noLocationPermissions();
    }

    @Override
    public boolean isGPSProviderEnabled() {
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void destroy() {
        stopLocationListening();
        view = null;
        locationListener = null;
        locationManager = null;
    }

    private void sendLocation(Location location) {
        if (!LocationUtil.isBetterLocation(location, currentBestLocation)) {
            return;
        }

        if (location == null) {
            startGettingLocation(false);
            return;
        }

        currentBestLocation = location;

        if (view != null) {
            view.onLocationSuccess(location);
        }

        if (!thresholdReached(location)) {
            return;
        }

        stopLocationListening();
        if (view != null) {
            view.onGettingLocationEnd();
        }
    }

    private boolean thresholdReached(Location location) {
        return !untilThreshold || location.getAccuracy() < threshold;
    }

    private boolean noLocationPermissions() {
        Context context = view.getContext();
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) ||
                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED);
    }

    @SuppressLint("MissingPermission")
    private void startLocationListening() {
        if (locationManager == null || listenerRegistered) {
            return;
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_REQUEST_INTERVAL_MS,
                    LOCATION_MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper()
            );
            listenerRegistered = true;
        } catch (SecurityException ignored) {
        }
    }

    private void stopLocationListening() {
        if (!listenerRegistered || locationManager == null || locationListener == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        }
        listenerRegistered = false;
    }

    private static class MyLocationListener implements LocationListener {
        private final WeakReference<LocationGettingPresenter> presenterRef;

        MyLocationListener(LocationGettingPresenter presenter) {
            this.presenterRef = new WeakReference<>(presenter);
        }

        @Override
        public void onLocationChanged(Location location) {
            LocationGettingPresenter p = presenterRef.get();
            if (p != null && location != null) {
                p.sendLocation(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    }
}
