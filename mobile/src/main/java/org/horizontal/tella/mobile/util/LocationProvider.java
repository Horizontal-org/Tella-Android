package org.horizontal.tella.mobile.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;


public class LocationProvider {
    public interface LocationCallback {
        void onNewLocationAvailable(@Nullable Location location);
    }

    public static void requestSingleUpdate(final Context context, final LocationCallback callback) {
        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (lm == null) {
            callback.onNewLocationAvailable(null);
            return;
        }

        boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!PermissionUtil.checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                !PermissionUtil.checkPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            callback.onNewLocationAvailable(null);
            return;
        }

        if (isNetworkEnabled) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            lm.requestSingleUpdate(criteria, new SimpleLocationListener(callback), null);
        } else {
            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                lm.requestSingleUpdate(criteria, new SimpleLocationListener(callback), null);
            } else {
                callback.onNewLocationAvailable(null);
            }
        }
    }

    public static void openSettings(final Context context) {
        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    public static boolean isLocationEnabled(Context context) {
        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return lm != null &&
                (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    private static class SimpleLocationListener implements LocationListener {
        final LocationCallback callback;

        SimpleLocationListener(LocationCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onLocationChanged(Location location) {
            callback.onNewLocationAvailable(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}

