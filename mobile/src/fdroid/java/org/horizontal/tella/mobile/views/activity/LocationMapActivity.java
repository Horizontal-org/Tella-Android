package org.horizontal.tella.mobile.views.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hzontal.tella_vault.MyLocation;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.databinding.ActivityLocationMapBinding;
import org.horizontal.tella.mobile.mvp.contract.ILocationGettingPresenterContract;
import org.horizontal.tella.mobile.mvp.presenter.LocationGettingPresenter;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.util.LocationUtil;

/**
 * F-Droid implementation: location picker without Google Maps.
 * Shows coordinates and uses LocationGettingPresenter; same intent contract as playstore variant.
 */
public class LocationMapActivity extends MetadataActivity implements ILocationGettingPresenterContract.IView {

    public static final String SELECTED_LOCATION = "sl";
    public static final String CURRENT_LOCATION_ONLY = "ro";

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView hint;
    private TextView latText;
    private TextView lngText;
    private LinearLayout coordsContainer;
    private FloatingActionButton faButton;

    @Nullable
    private MyLocation myLocation;
    private LocationGettingPresenter locationGettingPresenter;
    private boolean readOnly;
    private ActivityLocationMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLocationMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();

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

        if (!readOnly) {
            hint.setVisibility(View.VISIBLE);
        }

        if (myLocation != null) {
            showMyLocation(myLocation);
        } else if (readOnly) {
            startGettingLocation();
        }

        faButton.setOnClickListener(view -> {
            if (locationGettingPresenter.isGPSProviderEnabled()) {
                startGettingLocation();
            } else {
                maybeChangeTemporaryTimeout(() -> {
                    manageLocationSettings(C.GPS_PROVIDER, this::startGettingLocation);
                    return null;
                });
            }
        });
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

    @Override
    public void onGettingLocationStart() {
        progressBar.setVisibility(View.VISIBLE);
        coordsContainer.setVisibility(View.GONE);
    }

    @Override
    public void onGettingLocationEnd() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLocationSuccess(Location location) {
        if (location != null) {
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
        // no-op
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showMyLocation(MyLocation location) {
        coordsContainer.setVisibility(View.VISIBLE);
        latText.setText(String.format(getString(R.string.collect_form_geopoint_meta_latitude),
                LocationUtil.printCoordinate(location.getLatitude(), true)));
        lngText.setText(String.format(getString(R.string.collect_form_geopoint_meta_longitude),
                LocationUtil.printCoordinate(location.getLongitude(), false)));
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
        if (myLocation == null) {
            setCancelAndFinish();
        } else {
            setResult(Activity.RESULT_OK, new Intent().putExtra(SELECTED_LOCATION, myLocation));
            finish();
        }
    }

    private void setCancelAndFinish() {
        setResult(Activity.RESULT_CANCELED, new Intent().putExtra(SELECTED_LOCATION, myLocation));
        finish();
    }

    private void initView() {
        toolbar = binding.toolbar;
        progressBar = binding.content.progressBar;
        hint = binding.content.info;
        coordsContainer = binding.content.coordsContainer;
        latText = binding.content.latText;
        lngText = binding.content.lngText;
        faButton = binding.fabButton;
    }
}
