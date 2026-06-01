package org.horizontal.tella.mobile.views.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hzontal.tella_vault.MyLocation;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.databinding.ActivityLocationMapBinding;
import org.horizontal.tella.mobile.mvp.contract.ILocationGettingPresenterContract;
import org.horizontal.tella.mobile.mvp.presenter.LocationGettingPresenter;
import org.horizontal.tella.mobile.util.C;
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;

public class LocationMapActivity extends MetadataActivity implements ILocationGettingPresenterContract.IView {
    public static final String SELECTED_LOCATION = "sl";
    public static final String CURRENT_LOCATION_ONLY = "ro";

    private static final byte DEFAULT_LOCATION_ZOOM = 15;
    private static final float SELECTED_LOCATION_RADIUS_PX = 14f;

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView hint;
    private FloatingActionButton faButton;
    private MapView mapView;

    @Nullable
    private MyLocation myLocation;

    @Nullable
    private FixedPixelCircle selectedLocationCircle;
    @Nullable
    private TileDownloadLayer tileDownloadLayer;

    private LocationGettingPresenter locationGettingPresenter;
    private boolean readOnly;
    private ActivityLocationMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidGraphicFactory.createInstance(getApplication());
        super.onCreate(savedInstanceState);

        binding = ActivityLocationMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();

        myLocation = (MyLocation) getIntent().getSerializableExtra(SELECTED_LOCATION);
        readOnly = getIntent().getBooleanExtra(CURRENT_LOCATION_ONLY, true);
        locationGettingPresenter = new LocationGettingPresenter(this, false);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_form_geopoint_app_bar);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
        }

        configureMap();

        faButton.setOnClickListener(view -> {
            if (locationGettingPresenter.isGPSProviderEnabled()
                    || locationGettingPresenter.isNetworkProviderEnabled()) {
                startGettingLocation();
            } else {
                checkLocationSettings(C.GPS_PROVIDER, this::startGettingLocation);
            }
        });

        initMapLocationAndCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            myLocation = null;
            setCancelAndFinish();
            return true;
        }

        if (id == R.id.menu_item_select) {
            setResultAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tileDownloadLayer != null) {
            tileDownloadLayer.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (tileDownloadLayer != null) {
            tileDownloadLayer.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        locationGettingPresenter.destroy();
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onGettingLocationStart() {
        progressBar.setVisibility(View.VISIBLE);
        if (Preferences.isShowMapFirstLoadSheet()) {
            Preferences.setShowMapFirstLoadSheet(false);
            BottomSheetUtils.showStandardSheet(
                    getSupportFragmentManager(),
                    getString(R.string.collect_form_geopoint_first_load_title),
                    getString(R.string.collect_form_geopoint_first_load_message),
                    getString(R.string.action_ok),
                    null,
                    null,
                    null
            );
        }
    }

    @Override
    public void onGettingLocationEnd() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLocationSuccess(Location location) {
        if (location != null && virginMap()) {
            myLocation = MyLocation.fromLocation(location);
            showMyLocation(myLocation);
        }
    }

    @Override
    public void onNoLocationPermissions() {
        setCancelAndFinish();
    }

    @Override
    public void onGPSProviderDisabled() {
        showGpsMetadataDialog(C.GPS_PROVIDER, this::startGettingLocation);
    }

    @Override
    protected int getGpsMetadataDialogMessageResId() {
        return R.string.collect_form_geopoint_enable_gps_dialog_expl;
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void configureMap() {
        mapView.getMapScaleBar().setVisible(false);
        mapView.setBuiltInZoomControls(true);
        mapView.getModel().displayModel.setFixedTileSize(256);

        TileCache tileCache = AndroidUtil.createTileCache(
                this,
                "tella-location-map",
                mapView.getModel().displayModel.getTileSize(),
                1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor()
        );

        OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent(getPackageName());

        tileDownloadLayer = new TileDownloadLayer(
                tileCache,
                mapView.getModel().mapViewPosition,
                tileSource,
                AndroidGraphicFactory.INSTANCE
        );
        mapView.getLayerManager().getLayers().add(tileDownloadLayer);
        mapView.getLayerManager().getLayers().add(new MapLongPressLayer());
        tileDownloadLayer.start();

        byte minZoomLevel = tileSource.getZoomLevelMin();
        byte maxZoomLevel = tileSource.getZoomLevelMax();
        byte defaultZoomLevel = (byte) Math.max(minZoomLevel, maxZoomLevel - 1);

        mapView.setZoomLevelMin(minZoomLevel);
        mapView.setZoomLevelMax(maxZoomLevel);
        mapView.setCenter(new LatLong(0, 0));
        mapView.setZoomLevel(defaultZoomLevel);
    }

    private void showMyLocation(@NonNull MyLocation location) {
        LatLong selectedLatLong = new LatLong(location.getLatitude(), location.getLongitude());

        if (selectedLocationCircle == null) {
            selectedLocationCircle = new FixedPixelCircle(
                    selectedLatLong,
                    SELECTED_LOCATION_RADIUS_PX,
                    createSelectedLocationFillPaint(),
                    createSelectedLocationStrokePaint()
            );
            mapView.getLayerManager().getLayers().add(selectedLocationCircle);
        } else {
            selectedLocationCircle.setLatLong(selectedLatLong);
        }

        byte currentZoom = mapView.getModel().mapViewPosition.getZoomLevel();
        mapView.setZoomLevel((byte) Math.max(DEFAULT_LOCATION_ZOOM, currentZoom));
        mapView.getModel().mapViewPosition.animateTo(selectedLatLong);
        selectedLocationCircle.requestRedraw();
    }

    private void initMapLocationAndCamera() {
        if (!readOnly) {
            hint.setVisibility(View.VISIBLE);
        }

        if (myLocation == null || readOnly) {
            locationGettingPresenter.startGettingLocation(!readOnly);
        }

        if (myLocation != null) {
            showMyLocation(myLocation);
        }
    }

    private boolean virginMap() {
        return selectedLocationCircle == null;
    }

    private void startGettingLocation() {
        locationGettingPresenter.startGettingLocation(!readOnly);
    }

    private void setResultAndFinish() {
        if (myLocation == null) {
            setCancelAndFinish();
            return;
        }

        setResult(Activity.RESULT_OK, new Intent().putExtra(SELECTED_LOCATION, myLocation));
        finish();
    }

    private void setCancelAndFinish() {
        setResult(Activity.RESULT_CANCELED, new Intent().putExtra(SELECTED_LOCATION, myLocation));
        finish();
    }

    private void initView() {
        toolbar = binding.toolbar;
        progressBar = binding.content.progressBar;
        hint = binding.content.info;
        faButton = binding.fabButton;
        mapView = binding.content.mapView;
    }

    private Paint createSelectedLocationFillPaint() {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(AndroidGraphicFactory.INSTANCE.createColor(180, 230, 106, 35));
        paint.setStyle(Style.FILL);
        return paint;
    }

    private Paint createSelectedLocationStrokePaint() {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(AndroidGraphicFactory.INSTANCE.createColor(255, 230, 106, 35));
        paint.setStrokeWidth(3);
        paint.setStyle(Style.STROKE);
        return paint;
    }

    private class MapLongPressLayer extends Layer {
        @Override
        public void draw(
                BoundingBox boundingBox,
                byte zoomLevel,
                Canvas canvas,
                Point topLeftPoint,
                Rotation rotation
        ) {
        }

        @Override
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            if (readOnly || tapLatLong == null) {
                return false;
            }

            myLocation = new MyLocation();
            myLocation.setLatitude(tapLatLong.latitude);
            myLocation.setLongitude(tapLatLong.longitude);
            showMyLocation(myLocation);
            return true;
        }
    }
}
