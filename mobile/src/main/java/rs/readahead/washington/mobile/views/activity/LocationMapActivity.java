package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hzontal.tella_vault.MyLocation;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.mvp.contract.ILocationGettingPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.LocationGettingPresenter;
import rs.readahead.washington.mobile.util.C;


public class LocationMapActivity extends MetadataActivity implements
        /*OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraMoveStartedListener,
        MapEventsReceiver,*/
        ILocationGettingPresenterContract.IView {
    public static final String SELECTED_LOCATION = "sl";
    public static final String CURRENT_LOCATION_ONLY = "ro";

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 151;

   // private GoogleMap mMap;
    private MapView map;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.info)
    TextView hint;
    @BindView(R.id.fab_button)
    FloatingActionButton faButton;

    @Nullable
    private MyLocation myLocation;
    private Marker selectedMarker;
    private boolean virginMap = true;
    private LocationGettingPresenter locationGettingPresenter;
    private boolean readOnly;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_map);
        ButterKnife.bind(this);

        myLocation = (MyLocation) getIntent().getSerializableExtra(SELECTED_LOCATION);
        readOnly = getIntent().getBooleanExtra(CURRENT_LOCATION_ONLY, true);
        locationGettingPresenter = new LocationGettingPresenter(this, readOnly);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_form_geopoint_app_bar);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);
        }

        Context ctx = this.getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(18.0);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET
        });
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);

        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        GeoPoint point = new GeoPoint(45.845557, 26.170010);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(point);
        startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER);
        map.getOverlays().add(startMarker);

        map.getController().setCenter(point);

       /* SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);*/

        faButton.setOnClickListener(view -> {
                    if (locationGettingPresenter.isGPSProviderEnabled()) {
                        startGettingLocation();
                    } else {
                        manageLocationSettings(C.GPS_PROVIDER, this::startGettingLocation);
                    }
                }
        );
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
    protected void onDestroy() {
        super.onDestroy();
        locationGettingPresenter.destroy();
    }
/*
    @Override
    public void onMapReady(GoogleMap googleMap) {
       // mMap = googleMap;
        //mMap.setOnMapLongClickListener(this);

        initMapLocationAndCamera();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (readOnly) {
            return;
        }

        myLocation = new MyLocation();
        myLocation.setLatitude(latLng.latitude);
        myLocation.setLongitude(latLng.longitude);
        showMyLocation(myLocation);
    }

    @Override
    public void onCameraMoveStarted(int i) {
        virginMap = false;
    }
*/
    @Override
    public void onGettingLocationStart() {
       // mMap.getUiSettings().setScrollGesturesEnabled(false);
      //  mMap.getUiSettings().setZoomGesturesEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGettingLocationEnd() {
      //  mMap.getUiSettings().setScrollGesturesEnabled(true);
      //  mMap.getUiSettings().setZoomGesturesEnabled(true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLocationSuccess(Location location) {
        if (location != null && virginMap) {
            virginMap = false;
            myLocation = MyLocation.fromLocation(location);
            showMyLocation(myLocation);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onNoLocationPermissions() {
        setCancelAndFinish();
    }

    @Override
    public void onGPSProviderDisabled() {

    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showMyLocation(@NonNull MyLocation myLocation) {
       // LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());

        GeoPoint point = new GeoPoint(myLocation.getLatitude(), myLocation.getLongitude());

        Marker startMarker = new Marker(map);
        startMarker.setPosition(point);
        startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER);
        map.getOverlays().add(startMarker);

        map.getController().setCenter(point);

        if (selectedMarker == null) {
            selectedMarker = startMarker; //mMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.collect_form_geopoint_marker_content_desc)));
        } else {
            //selectedMarker.setPosition(latLng);
        }
        map.getController().animateTo(point);
        selectedMarker.setDraggable(!readOnly);


        /*float currentZoom = mMap.getCameraPosition().zoom;

       CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .bearing(0)
                .tilt(0)
                .zoom(Math.max(15f, currentZoom))
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
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

    private void startGettingLocation() {
        locationGettingPresenter.startGettingLocation(!readOnly);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == C.GPS_PROVIDER && resultCode == RESULT_OK) {
                startGettingLocation();
        }
    }

    private void setResultAndFinish() {
        if (selectedMarker == null) {
            setCancelAndFinish();
        } else {
            myLocation = new MyLocation();
            myLocation.setLatitude(selectedMarker.getPosition().getLatitude());
            myLocation.setLongitude(selectedMarker.getPosition().getLongitude());
            setResult(Activity.RESULT_OK, new Intent().putExtra(SELECTED_LOCATION, myLocation));
            finish();
        }
    }

    private void setCancelAndFinish() {
        setResult(Activity.RESULT_CANCELED, new Intent().putExtra(SELECTED_LOCATION, myLocation));
        finish();
    }
}
