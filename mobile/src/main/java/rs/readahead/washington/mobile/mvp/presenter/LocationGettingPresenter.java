package rs.readahead.washington.mobile.mvp.presenter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.lang.ref.WeakReference;

import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.mvp.contract.ILocationGettingPresenterContract;
import rs.readahead.washington.mobile.util.LocationUtil;


public class LocationGettingPresenter implements ILocationGettingPresenterContract.IPresenter {
    private static final long LOCATION_REQUEST_INTERVAL = 100; // very aggressive

    private ILocationGettingPresenterContract.IView view;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private boolean listenerRegistered;
    private boolean untilThreshold;
    private float threshold;
    private Location currentBestLocation;


    public LocationGettingPresenter(ILocationGettingPresenterContract.IView view, boolean untilThreshold) {
        this.view = view;
        this.untilThreshold = untilThreshold;
        threshold = Preferences.getLocationAccuracyThreshold();
        locationCallback = new MyLocationCallback(this);
        locationManager = (LocationManager) view.getContext().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(
                view.getContext().getApplicationContext());
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
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new MySuccessListener(this));
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
        locationCallback = null;
        locationManager = null;
        fusedLocationProviderClient = null;
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
            fusedLocationProviderClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
            listenerRegistered = true;
        }
    }

    private void stopLocationListening() {
        if (listenerRegistered) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            listenerRegistered = false;
        }
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private static class MySuccessListener implements OnSuccessListener<Location> {
        private WeakReference<LocationGettingPresenter> presenter;

        MySuccessListener(LocationGettingPresenter presenter) {
            this.presenter = new WeakReference<>(presenter);
        }

        @Override
        public void onSuccess(Location location) {
            if (presenter.get() != null) {
                presenter.get().sendLocation(location);
            }
        }
    }

    private static class MyLocationCallback extends LocationCallback {
        private WeakReference<LocationGettingPresenter> presenter;

        MyLocationCallback(LocationGettingPresenter presenter) {
            this.presenter = new WeakReference<>(presenter);
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();

            if (presenter.get() != null) {
                presenter.get().sendLocation(location);
            }
        }
    }
}
