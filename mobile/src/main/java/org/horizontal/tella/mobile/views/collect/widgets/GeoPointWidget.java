package org.horizontal.tella.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.MyLocation;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.bus.event.GPSProviderRequiredEvent;
import org.horizontal.tella.mobile.bus.event.LocationPermissionRequiredEvent;
import org.horizontal.tella.mobile.mvp.contract.ILocationGettingPresenterContract;
import org.horizontal.tella.mobile.mvp.presenter.LocationGettingPresenter;
import org.horizontal.tella.mobile.odk.FormController;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.util.LocationUtil;
import org.horizontal.tella.mobile.views.activity.LocationMapActivity;


/**
 * Based on ODK GeoPointWidget.
 */
@SuppressLint("ViewConstructor")
public class GeoPointWidget extends QuestionWidget implements ILocationGettingPresenterContract.IView {
    private static final String APPEARANCE_NONE = "none";
    private static final String APPEARANCE_MAP = "map";
    private static final String APPEARANCE_PLACEMENT_MAP = "placement-map";

    ImageButton selectButton;
    ImageButton clearButton;
    ProgressBar progressBar;
    TextView latitude;
    TextView longitude;
    TextView altitude;
    TextView accuracy;

    private String locationString;
    private String appearance;
    private LocationGettingPresenter locationGettingPresenter;
    private boolean locationGathering;


    public GeoPointWidget(Context context, @NonNull FormEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

        appearance = formEntryPrompt.getAppearanceHint();
        if (TextUtils.isEmpty(appearance)) {
            appearance = APPEARANCE_NONE;
        }

        locationString = formEntryPrompt.getAnswerText();
        locationGettingPresenter = new LocationGettingPresenter(this, true);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addGeoPointWidgetViews(linearLayout);
        addAnswerView(linearLayout);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        locationGettingPresenter.destroy();
    }

    @Override
    public IAnswerData getAnswer() {
        if (TextUtils.isEmpty(locationString)) {
            return null;
        }

        try {
            MyLocation myLocation = parseLocationString();

            return new GeoPointData(new double[] {
                    myLocation.getLatitude(),
                    myLocation.getLongitude(),
                    myLocation.getAltitude() != null ? myLocation.getAltitude() : 0d,
                    myLocation.getAccuracy() != null ? myLocation.getAccuracy() : 0d
            });
        } catch (Exception numberFormatException) {
            return null;
        }
    }

    @Override
    public void clearAnswer() {
        locationString = null;
        hideLocation();
    }

    @Override
    public void setFocus(Context context) {
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        MyLocation myLocation = (MyLocation) data;

        locationString = String.format(Locale.ROOT, "%s %s %s %s",
                myLocation.getLatitude(),
                myLocation.getLongitude(),
                myLocation.getAltitude() != null ? myLocation.getAltitude() : "0",
                myLocation.getAccuracy() != null ? myLocation.getAccuracy() : "0");

        showLocation();

        return locationString;
    }

    @Nullable
    @Override
    public String getBinaryName() {
        return locationString;
    }

    @Override
    public void onGettingLocationStart() {
        locationGathering = true;
        progressBar.setVisibility(VISIBLE);
        setButtons();
    }

    @Override
    public void onGettingLocationEnd() {
        locationGathering = false;
        progressBar.setVisibility(GONE);
        setButtons();
    }

    @Override
    public void onLocationSuccess(Location location) {
        setBinaryData(MyLocation.fromLocation(location));
    }

    @Override
    public void onNoLocationPermissions() {
        MyApplication.bus().post(new LocationPermissionRequiredEvent());
    }

    @Override
    public void onGPSProviderDisabled() {
        MyApplication.bus().post(new GPSProviderRequiredEvent());
    }

    private void addGeoPointWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.collect_widget_geo_point, linearLayout, true);

        selectButton = addButton(R.drawable.gps_fixed_icon_white);
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setContentDescription(getContext().getString(R.string.action_show_location));
        selectButton.setEnabled(!isReadonly());
        selectButton.setOnClickListener(v -> {
            if (APPEARANCE_MAP.equalsIgnoreCase(appearance)) {
                showLocationMapActivity(true);
            } else if (APPEARANCE_PLACEMENT_MAP.equalsIgnoreCase(appearance)) {
                showLocationMapActivity(false);
            } else {
                if (locationGathering) {
                    stopLocationGathering();
                } else {
                    startLocationGathering();
                }
            }
        });

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!isReadonly());
        clearButton.setOnClickListener(v -> clearAnswer());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));

        progressBar = view.findViewById(R.id.progressBar);

        latitude = view.findViewById(R.id.latitude);
        longitude = view.findViewById(R.id.longitude);
        altitude = view.findViewById(R.id.altitude);
        accuracy = view.findViewById(R.id.accuracy);

        if (locationString != null) {
            showLocation();
        } else {
            hideLocation();
        }
    }

    private void showLocationMapActivity(boolean currentLocationOnly) {
        try {
            if (!locationGettingPresenter.isLocationPermissionAllowed()) {
                onNoLocationPermissions();
                return;
            }

            if (!locationGettingPresenter.isGPSProviderEnabled()) {
                onGPSProviderDisabled();
                return;
            }

            Activity activity = (Activity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            MyLocation myLocation = locationString != null ? parseLocationString() : null;

            activity.startActivityForResult(new Intent(getContext(), LocationMapActivity.class)
                            .putExtra(LocationMapActivity.SELECTED_LOCATION, myLocation)
                            .putExtra(LocationMapActivity.CURRENT_LOCATION_ONLY, currentLocationOnly),
                    C.SELECTED_LOCATION
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showLocation() {
        setButtons();

        MyLocation myLocation = parseLocationString();

        latitude.setVisibility(VISIBLE);
        latitude.setText(String.format(getContext().getString(R.string.collect_form_geopoint_meta_latitude),
                LocationUtil.printCoordinate(myLocation.getLatitude(), true)));

        longitude.setVisibility(VISIBLE);
        longitude.setText(String.format(getContext().getString(R.string.collect_form_geopoint_meta_longitude),
                LocationUtil.printCoordinate(myLocation.getLongitude(), false)));

        altitude.setVisibility(VISIBLE);
        altitude.setText(String.format(getContext().getString(R.string.collect_form_geopoint_meta_altitude),
                String.format(Locale.ROOT, "%.02f", myLocation.getAltitude())));

        accuracy.setVisibility(VISIBLE);
        accuracy.setText(String.format(getContext().getString(R.string.collect_form_geopoint_meta_accuracy),
                myLocation.getAccuracy().toString()));
    }

    private void hideLocation() {
        setButtons();

        latitude.setVisibility(GONE);
        latitude.setText("");

        longitude.setVisibility(GONE);
        longitude.setText("");

        altitude.setVisibility(GONE);
        altitude.setText("");

        accuracy.setVisibility(GONE);
        accuracy.setText("");
    }

    private void setButtons() {
        if (locationString != null) {
            if (APPEARANCE_NONE.equalsIgnoreCase(appearance)) {
                selectButton.setVisibility(locationGathering ? VISIBLE : GONE);
                clearButton.setVisibility(locationGathering ? GONE : VISIBLE);
            } else {
                selectButton.setVisibility(GONE);
                clearButton.setVisibility(VISIBLE);
            }
        } else {
            selectButton.setVisibility(VISIBLE);
            clearButton.setVisibility(GONE);
        }

        // specific map appearances have fixed select string
        if (APPEARANCE_NONE.equalsIgnoreCase(appearance)) {
            selectButton.setImageDrawable(locationGathering ?
                    getResources().getDrawable(R.drawable.ic_stop_red) :
                    getResources().getDrawable(R.drawable.gps_fixed_icon_white));
        }
    }

    private MyLocation parseLocationString() {
        String[] sa = locationString.split(" ");

        MyLocation myLocation = new MyLocation();
        myLocation.setLatitude(Double.parseDouble(sa[0]));
        myLocation.setLongitude(Double.parseDouble(sa[1]));
        myLocation.setAltitude(Double.parseDouble(sa[2]));
        myLocation.setAccuracy(Float.parseFloat(sa[3]));

        return myLocation;
    }

    private void startLocationGathering() {
        locationGettingPresenter.startGettingLocation(false);
    }

    private void stopLocationGathering() {
        locationGettingPresenter.stopGettingLocation();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isReadonly() {
        return formEntryPrompt.isReadOnly();
    }
}
