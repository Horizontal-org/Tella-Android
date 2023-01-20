package rs.readahead.washington.mobile.mvp.presenter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.mvp.contract.ILocationGettingPresenterContract;
import rs.readahead.washington.mobile.util.LocationUtil;


public class LocationGettingPresenter implements ILocationGettingPresenterContract.IPresenter, IMyLocationConsumer {
    private static final long LOCATION_REQUEST_INTERVAL = 100; // very aggressive

    private ILocationGettingPresenterContract.IView view;
    private LocationManager locationManager;
    private GpsMyLocationProvider locationProvider;
    private boolean listenerRegistered;
    private final boolean untilThreshold;
    private final float threshold;
    private Location currentBestLocation;


    public LocationGettingPresenter(ILocationGettingPresenterContract.IView view, boolean untilThreshold) {
        this.view = view;
        this.untilThreshold = untilThreshold;
        threshold = Preferences.getLocationAccuracyThreshold();

        locationManager = (LocationManager) view.getContext().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        locationProvider = new GpsMyLocationProvider(view.getContext());
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

        if (useLastKnownLocation) {
            sendLocation(locationProvider.getLastKnownLocation());
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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void destroy() {
        stopLocationListening();

        // throw them some NPEs on calls after destroy..
        view = null;
        locationManager = null;
        locationProvider = null;
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
        if (!listenerRegistered) {
            locationProvider.startLocationProvider(this);
            listenerRegistered = true;
        }
    }

    private void stopLocationListening() {
        if (listenerRegistered) {
            locationProvider.stopLocationProvider();
            listenerRegistered = false;
        }
    }

    @Override
    public void onLocationChanged(Location location, IMyLocationProvider source) {
        sendLocation(location);
    }
}
