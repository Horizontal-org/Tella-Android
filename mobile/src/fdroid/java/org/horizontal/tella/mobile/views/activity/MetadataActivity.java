package org.horizontal.tella.mobile.views.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.subjects.BehaviorSubject;
import kotlin.Unit;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract;
import org.horizontal.tella.mobile.presentation.entity.SensorData;
import org.horizontal.tella.mobile.util.DialogsUtil;
import org.horizontal.tella.mobile.util.LocationUtil;
import org.horizontal.tella.mobile.util.MetadataUtils;
import org.horizontal.tella.mobile.util.TelephonyUtils;
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity;

/**
 * F-Droid implementation: uses only Android framework LocationManager (no Google Play Services).
 */
public abstract class MetadataActivity extends BaseLockActivity implements SensorEventListener {
    private static final long LOCATION_REQUEST_INTERVAL = 5000;
    private final static SensorData lightSensorData = new SensorData();
    private final static SensorData ambientTemperatureSensorData = new SensorData();
    private final static BehaviorSubject<MyLocation> locationSubject = BehaviorSubject.create();
    private static Location currentBestLocation;
    private final BehaviorSubject<List<String>> wifiSubject = BehaviorSubject.create();
    private SensorManager mSensorManager;
    private Sensor mLight;
    private Sensor mAmbientTemperature;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanResultReceiver;
    private boolean locationListenerRegistered = false;
    private boolean wifiReceiverRegistered = false;
    private boolean sensorListenerRegistered = false;

    private AlertDialog metadataAlertDialog;
    private AlertDialog locationAlertDialog;
    private Relay<MetadataHolder> metadataCancelRelay;
    private CompositeDisposable disposables;
    private boolean inProgress = false;

    private static void acceptBetterLocation(Location location) {
        if (!LocationUtil.isBetterLocation(location, currentBestLocation)) {
            return;
        }
        currentBestLocation = location;
        locationSubject.onNext(MyLocation.fromLocation(location));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAmbientTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                acceptBetterLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));
            }
        };

        metadataCancelRelay = PublishRelay.create();
        disposables = new CompositeDisposable();
    }

    private List<String> getWifiStrings(List<ScanResult> results) {
        List<String> wifiStrings = new ArrayList<>(results.size());
        for (ScanResult result : results) {
            wifiStrings.add(result.SSID);
        }
        return wifiStrings;
    }

    protected void startSensorListening() {
        if (Preferences.isAnonymousMode()) return;
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAmbientTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        sensorListenerRegistered = true;
    }

    public void startLocationMetadataListening() {
        if (Preferences.isAnonymousMode()) return;
        startLocationListening();
        startWifiListening();
    }

    @SuppressWarnings("MissingPermission")
    private synchronized void startLocationListening() {
        if (isFineLocationPermissionDenied() || locationManager == null) return;
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_REQUEST_INTERVAL,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
            );
            locationListenerRegistered = true;
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) acceptBetterLocation(last);
        } catch (SecurityException ignored) {}
    }

    private synchronized void startWifiListening() {
        if (isFineLocationPermissionDenied() || wifiManager == null || wifiReceiverRegistered) return;
        wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanResultReceiver, filter);
        wifiReceiverRegistered = true;
    }

    protected synchronized void startWifiScan() {
        if (Preferences.isAnonymousMode()) return;
        if (wifiManager != null && wifiReceiverRegistered) wifiManager.startScan();
    }

    private void stopSensorListening() {
        if (!sensorListenerRegistered) return;
        mSensorManager.unregisterListener(this);
        sensorListenerRegistered = false;
    }

    public void stopLocationMetadataListening() {
        stopLocationListening();
        stopWifiListening();
    }

    private synchronized void stopLocationListening() {
        if (!locationListenerRegistered || locationManager == null || locationListener == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {}
        locationListenerRegistered = false;
    }

    private synchronized void stopWifiListening() {
        if (!wifiReceiverRegistered) return;
        unregisterReceiver(wifiScanResultReceiver);
        wifiReceiverRegistered = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSensorListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSensorListening();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            lightSensorData.setValue(event.timestamp, event.values[0]);
        } else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ambientTemperatureSensorData.setValue(event.timestamp, event.values[0]);
        }
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) disposables.dispose();
        hideLocationAlertDialog();
        wifiSubject.onComplete();
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private boolean isFineLocationPermissionDenied() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
    }

    private boolean isLocationProviderEnabled() {
        if (locationManager == null) return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    protected void checkLocationSettings(final int requestCode, final LocationSettingsCheckDoneListener listener) {
        if (isFineLocationPermissionDenied()) {
            listener.onContinue();
            return;
        }
        if (!Preferences.isAnonymousMode() && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsMetadataDialog(requestCode, listener);
        } else {
            listener.onContinue();
        }
    }

    protected void manageLocationSettings(final int requestCode, final LocationSettingsCheckDoneListener listener) {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        listener.onContinue();
    }

    protected void showGpsMetadataDialog(final int requestCode, final LocationSettingsCheckDoneListener listener) {
        maybeChangeTemporaryTimeout(() -> {
            BottomSheetUtils.showConfirmSheet(
                    getSupportFragmentManager(),
                    getString(R.string.verification_prompt_dialog_title),
                    getString(R.string.verification_prompt_dialog_expl),
                    getString(R.string.verification_prompt_action_enable_GPS),
                    getString(R.string.verification_prompt_action_ignore),
                    isConfirmed -> {
                        if (isConfirmed) {
                            manageLocationSettings(requestCode, listener);
                        } else {
                            listener.onContinue();
                        }
                    }
            );
            return Unit.INSTANCE;
        });
    }

    public SensorData getLightSensorData() { return lightSensorData; }
    public SensorData getAmbientTemperatureSensorData() { return ambientTemperatureSensorData; }
    public Observable<List<String>> observeWifiData() { return wifiSubject; }
    public Observable<MyLocation> observeLocationData() { return locationSubject; }

    public Observable<MetadataHolder> observeMetadata() {
        return Observable.combineLatest(
                observeLocationData().startWith(MyLocation.createEmpty()),
                observeWifiData().startWith(Collections.<String>emptyList()),
                MetadataHolder::new
        )
                .filter(mh -> (!mh.getWifis().isEmpty() || !mh.getLocation().isEmpty()))
                .take((5 * 60 * 1000) / (int) LOCATION_REQUEST_INTERVAL)
                .takeUntil(mh -> !mh.getWifis().isEmpty() && !mh.getLocation().isEmpty());
    }

    public void attachMediaFileMetadata(final VaultFile vaultFile, final IMetadataAttachPresenterContract.IPresenter metadataAttacher) {
        if (Preferences.isAnonymousMode()) return;
        startWifiScan();

        final Metadata metadata = new Metadata();
        metadata.setFileName(vaultFile.name);
        metadata.setFileHashSHA256(vaultFile.hash);
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setAmbientTemperature(getAmbientTemperatureSensorData().hasValue() ? getAmbientTemperatureSensorData().getValue() : null);
        metadata.setLight(getLightSensorData().hasValue() ? getLightSensorData().getValue() : null);
        metadata.setDeviceID(MetadataUtils.getDeviceID());
        metadata.setWifiMac(MetadataUtils.getWifiMac());
        metadata.setIPv4(MetadataUtils.getIPv4());
        metadata.setIPv6(MetadataUtils.getIPv6());
        metadata.setDataType(MetadataUtils.getDataType(getBaseContext()));
        metadata.setNetwork(MetadataUtils.getNetwork(getBaseContext()));
        metadata.setNetworkType(MetadataUtils.getNetworkType(getBaseContext()));
        metadata.setHardware(MetadataUtils.getHardware());
        metadata.setManufacturer(MetadataUtils.getManufacturer());
        metadata.setScreenSize(MetadataUtils.getScreenSize(getBaseContext()));
        metadata.setLanguage(MetadataUtils.getLanguage());
        metadata.setLocale(MetadataUtils.getLocale());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            metadata.setCells(TelephonyUtils.getCellInfo(this));
        }

        if (!isLocationProviderEnabled() || isFineLocationPermissionDenied()) {
            metadataAttacher.attachMetadata(vaultFile, metadata);
            return;
        }

        disposables.add(observeMetadata()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showMetadataProgressBarDialog())
                .takeUntil(metadataCancelRelay)
                .doOnNext(data -> setInProgress(true))
                .doFinally(this::hideMetadataProgressBarDialog)
                .subscribeWith(new DisposableObserver<MetadataHolder>() {
                    @Override
                    public void onNext(@NonNull MetadataHolder value) {
                        if (!value.getWifis().isEmpty()) {
                            metadata.setWifis(value.getWifis());
                            networkGatheringChecked();
                        }
                        if (!value.getLocation().isEmpty()) {
                            metadata.setMyLocation(value.getLocation());
                            locationGahteringChecked();
                        }
                        if ((Settings.Global.getInt(getBaseContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0)
                                && wifiManager != null && !wifiManager.isWifiEnabled() && metadata.getWifis() == null) {
                            metadata.setWifis(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) { onComplete(); }

                    @Override
                    public void onComplete() {
                        metadataAttacher.attachMetadata(vaultFile, metadata);
                        setInProgress(false);
                    }
                })
        );
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    protected void showMetadataProgressBarDialog() {
        metadataAlertDialog = DialogsUtil.showMetadataProgressBarDialog(this, (dialog, which) ->
                metadataCancelRelay.accept(MetadataHolder.createEmpty()));
    }

    protected void setInProgress(boolean inProgress) { this.inProgress = inProgress; }
    protected boolean isInProgress() { return inProgress; }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    protected void hideMetadataProgressBarDialog() {
        if (metadataAlertDialog != null) metadataAlertDialog.dismiss();
    }

    private void networkGatheringChecked() {
        if (metadataAlertDialog != null) {
            metadataAlertDialog.findViewById(R.id.networkProgress).setVisibility(View.GONE);
            metadataAlertDialog.findViewById(R.id.networkCheck).setVisibility(View.VISIBLE);
        }
    }

    private void locationGahteringChecked() {
        if (metadataAlertDialog != null) {
            metadataAlertDialog.findViewById(R.id.locationProgress).setVisibility(View.GONE);
            metadataAlertDialog.findViewById(R.id.locationCheck).setVisibility(View.VISIBLE);
        }
    }

    protected void hideLocationAlertDialog() {
        if (locationAlertDialog != null) locationAlertDialog.dismiss();
    }

    interface LocationSettingsCheckDoneListener {
        void onContinue();
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    static class MetadataHolder {
        private final MyLocation location;
        private List<String> wifis;

        MetadataHolder(MyLocation location, List<String> wifis) {
            this.location = location;
            setWifis(wifis);
        }

        static MetadataHolder createEmpty() {
            return new MetadataHolder(MyLocation.createEmpty(), Collections.emptyList());
        }

        MyLocation getLocation() { return location; }
        List<String> getWifis() { return wifis; }

        private void setWifis(final List<String> wifis) {
            this.wifis = new ArrayList<>();
            for (String wifi : wifis) {
                if (!this.wifis.contains(wifi)) this.wifis.add(wifi);
            }
        }
    }
}
