package org.horizontal.tella.mobile.mvp.presenter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.mvp.contract.ILocationGettingPresenterContract;
import org.horizontal.tella.mobile.util.LocationUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LocationGettingPresenter implements ILocationGettingPresenterContract.IPresenter {
    private static final long LOCATION_REQUEST_INTERVAL_MS = 100;
    private static final float LOCATION_MIN_DISTANCE_M = 0f;
    private static final long LOCATION_TIMEOUT_MS = 15_000;

    private ILocationGettingPresenterContract.IView view;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private final List<String> activeProviders = new ArrayList<>();
    private final boolean untilThreshold;
    private final float threshold;
    private Location currentBestLocation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable locationTimeoutRunnable;

    public LocationGettingPresenter(ILocationGettingPresenterContract.IView view, boolean untilThreshold) {
        this.view = view;
        this.untilThreshold = untilThreshold;
        this.threshold = Preferences.getLocationAccuracyThreshold();
        this.locationManager = (LocationManager) view.getContext()
                .getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = new MyLocationListener(this);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void startGettingLocation(boolean useLastKnownLocation) {
        if (noLocationPermissions()) {
            view.onNoLocationPermissions();
            return;
        }

        if (!isGPSProviderEnabled() && !isNetworkProviderEnabled()) {
            view.onGPSProviderDisabled();
            return;
        }

        view.onGettingLocationStart();
        currentBestLocation = null;
        scheduleLocationTimeout();

        if (useLastKnownLocation && locationManager != null) {
            Location lastLocation = getBestLastKnownLocation();
            if (lastLocation != null) {
                sendLocation(lastLocation);
            }
        }

        startLocationListening();
    }

    @Override
    public void stopGettingLocation() {
        cancelLocationTimeout();
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

    public boolean isNetworkProviderEnabled() {
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void destroy() {
        cancelLocationTimeout();
        stopLocationListening();
        view = null;
        locationListener = null;
        locationManager = null;
    }

    private void sendLocation(Location location) {
        if (!LocationUtil.isBetterLocation(location, currentBestLocation)) {
            return;
        }

        currentBestLocation = location;

        if (view != null) {
            view.onLocationSuccess(location);
        }

        if (!thresholdReached(location)) {
            return;
        }

        stopGettingLocation();
    }

    private boolean thresholdReached(Location location) {
        return !untilThreshold || location.getAccuracy() < threshold;
    }

    private boolean noLocationPermissions() {
        Context context = view.getContext();
        boolean fineGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        return !fineGranted && !coarseGranted;
    }

    @SuppressLint("MissingPermission")
    private Location getBestLastKnownLocation() {
        if (locationManager == null) {
            return null;
        }

        Location best = null;
        for (String provider : getAvailableProviders()) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && LocationUtil.isBetterLocation(location, best)) {
                best = location;
            }
        }
        return best;
    }

    private List<String> getAvailableProviders() {
        List<String> providers = new ArrayList<>();
        if (locationManager == null) {
            return providers;
        }
        if (isGPSProviderEnabled()) {
            providers.add(LocationManager.GPS_PROVIDER);
        }
        if (isNetworkProviderEnabled()) {
            providers.add(LocationManager.NETWORK_PROVIDER);
        }
        return providers;
    }

    @SuppressLint("MissingPermission")
    private void startLocationListening() {
        if (locationManager == null) {
            return;
        }

        for (String provider : getAvailableProviders()) {
            if (activeProviders.contains(provider)) {
                continue;
            }
            try {
                locationManager.requestLocationUpdates(
                        provider,
                        LOCATION_REQUEST_INTERVAL_MS,
                        LOCATION_MIN_DISTANCE_M,
                        locationListener,
                        Looper.getMainLooper()
                );
                activeProviders.add(provider);
            } catch (SecurityException ignored) {
            }
        }
    }

    private void stopLocationListening() {
        if (locationManager == null || locationListener == null || activeProviders.isEmpty()) {
            activeProviders.clear();
            return;
        }

        for (String provider : activeProviders) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException ignored) {
            }
        }
        activeProviders.clear();
    }

    private void scheduleLocationTimeout() {
        cancelLocationTimeout();
        locationTimeoutRunnable = this::stopGettingLocation;
        handler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT_MS);
    }

    private void cancelLocationTimeout() {
        if (locationTimeoutRunnable != null) {
            handler.removeCallbacks(locationTimeoutRunnable);
            locationTimeoutRunnable = null;
        }
    }

    private static class MyLocationListener implements LocationListener {
        private final WeakReference<LocationGettingPresenter> presenterRef;

        MyLocationListener(LocationGettingPresenter presenter) {
            this.presenterRef = new WeakReference<>(presenter);
        }

        @Override
        public void onLocationChanged(Location location) {
            LocationGettingPresenter presenter = presenterRef.get();
            if (presenter != null && location != null) {
                presenter.sendLocation(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}
