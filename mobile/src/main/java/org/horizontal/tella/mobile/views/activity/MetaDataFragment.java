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
import android.os.Handler;
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

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract;
import org.horizontal.tella.mobile.presentation.entity.SensorData;
import org.horizontal.tella.mobile.util.DialogsUtil;
import org.horizontal.tella.mobile.util.LocationUtil;
import org.horizontal.tella.mobile.util.MetadataUtils;
import org.horizontal.tella.mobile.util.TelephonyUtils;
import org.horizontal.tella.mobile.views.base_ui.BaseFragment;
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

public abstract class MetaDataFragment extends BaseFragment implements SensorEventListener {
    private static final long LOCATION_REQUEST_INTERVAL = 5000;

    private static final SensorData lightSensorData = new SensorData();
    private static final SensorData ambientTemperatureSensorData = new SensorData();
    private static final BehaviorSubject<MyLocation> locationSubject = BehaviorSubject.create();

    private static Location currentBestLocation;

    private final BehaviorSubject<List<String>> wifiSubject = BehaviorSubject.create();

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor ambientTemperatureSensor;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanResultReceiver;
    private boolean locationListenerRegistered;
    private boolean wifiReceiverRegistered;
    private boolean sensorListenerRegistered;
    private boolean wifiScanReceived;

    private AlertDialog metadataAlertDialog;
    private AlertDialog locationAlertDialog;
    private Relay<MetadataActivity.MetadataHolder> metadataCancelRelay;
    private CompositeDisposable disposables;
    @Nullable
    private MetadataActivity.LocationSettingsCheckDoneListener pendingLocationSettingsListener;

    private static void acceptBetterLocation(Location location) {
        if (!LocationUtil.isBetterLocation(location, currentBestLocation)) {
            return;
        }

        currentBestLocation = location;
        locationSubject.onNext(MyLocation.fromLocation(location));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) baseActivity.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        ambientTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        locationManager = (LocationManager) baseActivity.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                acceptBetterLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                startLocationListening();
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };

        wifiManager = (WifiManager) baseActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ActivityCompat.checkSelfPermission(
                        baseActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                wifiScanReceived = true;
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
        if (Preferences.isAnonymousMode()) {
            return;
        }

        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, ambientTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorListenerRegistered = true;
    }

    public void startLocationMetadataListening() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        startLocationListening();
        startWifiListening();
    }

    @SuppressWarnings("MissingPermission")
    private synchronized void startLocationListening() {
        if (isFineLocationPermissionDenied() || locationManager == null) {
            return;
        }

        requestLocationUpdatesIfEnabled(LocationManager.GPS_PROVIDER);
        requestLocationUpdatesIfEnabled(LocationManager.NETWORK_PROVIDER);
        locationListenerRegistered = true;
        acceptBestLastKnownLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdatesIfEnabled(String provider) {
        if (locationManager == null || locationListener == null) {
            return;
        }

        try {
            if (!locationManager.isProviderEnabled(provider)) {
                return;
            }
            locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_REQUEST_INTERVAL,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
            );
        } catch (IllegalArgumentException | SecurityException ignored) {
        }
    }

    @SuppressWarnings("MissingPermission")
    private void acceptBestLastKnownLocation() {
        if (locationManager == null) {
            return;
        }

        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (!locationManager.isProviderEnabled(provider)) {
                    continue;
                }
                Location lastLocation = locationManager.getLastKnownLocation(provider);
                if (lastLocation != null && LocationUtil.isBetterLocation(lastLocation, best)) {
                    best = lastLocation;
                }
            } catch (IllegalArgumentException | SecurityException ignored) {
            }
        }

        if (best != null) {
            acceptBetterLocation(best);
        }
    }

    private synchronized void startWifiListening() {
        if (isFineLocationPermissionDenied() || wifiManager == null || wifiReceiverRegistered) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(
                baseActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        wifiSubject.onNext(getWifiStrings(wifiManager.getScanResults()));

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        baseActivity.registerReceiver(wifiScanResultReceiver, filter);
        wifiReceiverRegistered = true;
    }

    protected synchronized void startWifiScan() {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        if (wifiManager != null && wifiReceiverRegistered) {
            wifiManager.startScan();
        }
    }

    private void stopSensorListening() {
        if (!sensorListenerRegistered) {
            return;
        }

        sensorManager.unregisterListener(this);
        sensorListenerRegistered = false;
    }

    public void stopLocationMetadataListening() {
        stopLocationListening();
        stopWifiListening();
    }

    private synchronized void stopLocationListening() {
        if (!locationListenerRegistered || locationManager == null || locationListener == null) {
            return;
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        }
        locationListenerRegistered = false;
    }

    private synchronized void stopWifiListening() {
        if (!wifiReceiverRegistered) {
            return;
        }

        baseActivity.unregisterReceiver(wifiScanResultReceiver);
        wifiReceiverRegistered = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        startSensorListening();
        resumePendingLocationSettingsCheck();
    }

    @Override
    public void onPause() {
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
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }
        hideLocationAlertDialog();
        wifiSubject.onComplete();
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean isFineLocationPermissionDenied() {
        return ContextCompat.checkSelfPermission(baseActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED;
    }

    private boolean isLocationProviderEnabled() {
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    protected void checkLocationSettings(
            final int requestCode,
            final MetadataActivity.LocationSettingsCheckDoneListener listener
    ) {
        if (isFineLocationPermissionDenied()) {
            listener.onContinue();
            return;
        }

        if (!Preferences.isAnonymousMode() && !isLocationProviderEnabled()) {
            showGpsMetadataDialog(requestCode, listener);
        } else {
            listener.onContinue();
        }
    }

    protected void manageLocationSettings(
            final int requestCode,
            final MetadataActivity.LocationSettingsCheckDoneListener listener
    ) {
        if (isLocationProviderEnabled()) {
            pendingLocationSettingsListener = null;
            listener.onContinue();
            return;
        }

        pendingLocationSettingsListener = listener;
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void resumePendingLocationSettingsCheck() {
        if (pendingLocationSettingsListener == null || isFineLocationPermissionDenied() || !isLocationProviderEnabled()) {
            return;
        }

        MetadataActivity.LocationSettingsCheckDoneListener listener = pendingLocationSettingsListener;
        pendingLocationSettingsListener = null;
        listener.onContinue();
    }

    private void showGpsMetadataDialog(
            final int requestCode,
            final MetadataActivity.LocationSettingsCheckDoneListener listener
    ) {
        BottomSheetUtils.showConfirmSheet(
                baseActivity.getSupportFragmentManager(),
                getString(R.string.verification_prompt_dialog_title),
                getString(R.string.verification_prompt_dialog_expl),
                getString(R.string.verification_prompt_action_enable_GPS),
                getString(R.string.verification_prompt_action_ignore),
                isConfirmed -> {
                    if (isConfirmed) {
                        baseActivity.maybeChangeTemporaryTimeout(() -> {
                            manageLocationSettings(requestCode, listener);
                            return Unit.INSTANCE;
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(listener::onContinue);
                    }
                }
        );
    }

    public SensorData getLightSensorData() {
        return lightSensorData;
    }

    public SensorData getAmbientTemperatureSensorData() {
        return ambientTemperatureSensorData;
    }

    public Observable<List<String>> observeWifiData() {
        return wifiSubject;
    }

    public Observable<MyLocation> observeLocationData() {
        return locationSubject;
    }

    public Observable<MetadataActivity.MetadataHolder> observeMetadata() {
        if (!isLocationProviderEnabled()) {
            List<String> wifis = wifiSubject.hasValue()
                    ? wifiSubject.getValue()
                    : Collections.emptyList();
            return Observable.just(new MetadataActivity.MetadataHolder(MyLocation.createEmpty(), wifis));
        }

        return Observable.combineLatest(
                        observeLocationData().startWith(MyLocation.createEmpty()),
                        observeWifiData().startWith(Collections.<String>emptyList()),
                        MetadataActivity.MetadataHolder::new
                )
                .filter(mh -> !mh.getWifis().isEmpty() || !mh.getLocation().isEmpty())
                .take((5 * 60 * 1000) / (int) LOCATION_REQUEST_INTERVAL)
                .takeUntil(this::hasAllRequestedMetadata);
    }

    private boolean hasAllRequestedMetadata(MetadataActivity.MetadataHolder holder) {
        if (holder.getLocation().isEmpty()) {
            return false;
        }
        return !holder.getWifis().isEmpty() || wifiScanReceived || isWifiScanUnavailable();
    }

    private boolean isWifiScanUnavailable() {
        return MetadataActivity.isAirplaneModeOn(baseActivity)
                && wifiManager != null
                && !wifiManager.isWifiEnabled();
    }

    public void attachMediaFileMetadata(
            final VaultFile vaultFile,
            final IMetadataAttachPresenterContract.IPresenter metadataAttacher
    ) {
        if (Preferences.isAnonymousMode()) {
            return;
        }

        wifiScanReceived = false;
        startWifiScan();

        final Metadata metadata = new Metadata();
        metadata.setFileName(vaultFile.name);
        metadata.setFileHashSHA256(vaultFile.hash);
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setAmbientTemperature(
                getAmbientTemperatureSensorData().hasValue()
                        ? getAmbientTemperatureSensorData().getValue()
                        : null
        );
        metadata.setLight(getLightSensorData().hasValue() ? getLightSensorData().getValue() : null);
        metadata.setDeviceID(MetadataUtils.getDeviceID(baseActivity));
        metadata.setWifiMac(MetadataUtils.getWifiMac());
        metadata.setIPv4(MetadataUtils.getIPv4());
        metadata.setIPv6(MetadataUtils.getIPv6());
        metadata.setDataType(MetadataUtils.getDataType(baseActivity));
        metadata.setNetwork(MetadataUtils.getNetwork(baseActivity));
        metadata.setNetworkType(MetadataUtils.getNetworkType(baseActivity));
        metadata.setHardware(MetadataUtils.getHardware());
        metadata.setManufacturer(MetadataUtils.getManufacturer());
        metadata.setScreenSize(MetadataUtils.getScreenSize(baseActivity));
        metadata.setLanguage(MetadataUtils.getLanguage());
        metadata.setLocale(MetadataUtils.getLocale());

        if (ActivityCompat.checkSelfPermission(baseActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            metadata.setCells(TelephonyUtils.getCellInfo(baseActivity));
        }

        if (isFineLocationPermissionDenied()) {
            metadataAttacher.attachMetadata(vaultFile, metadata);
            return;
        }

        disposables.add(observeMetadata()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> showMetadataProgressBarDialog())
                .takeUntil(metadataCancelRelay)
                .doFinally(this::hideMetadataProgressBarDialog)
                .subscribeWith(new DisposableObserver<MetadataActivity.MetadataHolder>() {
                    @Override
                    public void onNext(@NonNull MetadataActivity.MetadataHolder value) {
                        if (!value.getWifis().isEmpty()) {
                            metadata.setWifis(value.getWifis());
                            networkGatheringChecked();
                        }

                        if (!value.getLocation().isEmpty()) {
                            metadata.setMyLocation(value.getLocation());
                            locationGahteringChecked();
                        }

                        if (Settings.Global.getInt(
                                baseActivity.getContentResolver(),
                                Settings.Global.AIRPLANE_MODE_ON,
                                0
                        ) != 0 && wifiManager != null && !wifiManager.isWifiEnabled()
                                && metadata.getWifis() == null) {
                            metadata.setWifis(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        onComplete();
                    }

                    @Override
                    public void onComplete() {
                        metadataAttacher.attachMetadata(vaultFile, metadata);
                    }
                }));
    }

    protected void showMetadataProgressBarDialog() {
        metadataAlertDialog = DialogsUtil.showMetadataProgressBarDialog(
                baseActivity,
                (dialog, which) -> metadataCancelRelay.accept(MetadataActivity.MetadataHolder.createEmpty())
        );
        applyExistingMetadataDialogChecks();
    }

    protected void hideMetadataProgressBarDialog() {
        if (metadataAlertDialog != null) {
            metadataAlertDialog.dismiss();
        }
    }

    private void applyExistingMetadataDialogChecks() {
        if (locationSubject.hasValue()
                && locationSubject.getValue() != null
                && !locationSubject.getValue().isEmpty()) {
            locationGahteringChecked();
        }
        if (wifiSubject.hasValue()
                && wifiSubject.getValue() != null
                && !wifiSubject.getValue().isEmpty()) {
            networkGatheringChecked();
        }
    }

    private void networkGatheringChecked() {
        setMetadataRowChecked(R.id.networkProgress, R.id.networkCheck);
    }

    private void locationGahteringChecked() {
        setMetadataRowChecked(R.id.locationProgress, R.id.locationCheck);
    }

    private void setMetadataRowChecked(int progressId, int checkId) {
        AlertDialog dialog = metadataAlertDialog;
        if (dialog == null) {
            return;
        }

        Runnable update = () -> {
            if (metadataAlertDialog == null) {
                return;
            }
            View progress = metadataAlertDialog.findViewById(progressId);
            View check = metadataAlertDialog.findViewById(checkId);
            if (progress != null) {
                progress.setVisibility(View.GONE);
            }
            if (check != null) {
                check.setVisibility(View.VISIBLE);
            }
        };

        View decor = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;
        if (decor != null) {
            decor.post(update);
        } else {
            update.run();
        }
    }

    protected void hideLocationAlertDialog() {
        if (locationAlertDialog != null) {
            locationAlertDialog.dismiss();
        }
    }

    @Override
    public void initView(@NonNull View view) {
    }
}
