package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.internal.LinkedTreeMap;
import com.hzontal.tella_vault.MyLocation;

import java.util.Locale;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.GPSProviderRequiredEvent;
import rs.readahead.washington.mobile.bus.event.LocationPermissionRequiredEvent;
import rs.readahead.washington.mobile.mvp.contract.ILocationGettingPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.LocationGettingPresenter;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziGeoData;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.LocationUtil;
import rs.readahead.washington.mobile.views.activity.LocationMapActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

/**
 * Based on ODK GeoPointWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziGeoPointWidget extends UwaziQuestionWidget implements ILocationGettingPresenterContract.IView {
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


    public UwaziGeoPointWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

        /*appearance = formEntryPrompt.getAppearanceHint();
        if (TextUtils.isEmpty(appearance)) {
            appearance = APPEARANCE_NONE;
        }*/
        //appearance = APPEARANCE_NONE;
        appearance = APPEARANCE_PLACEMENT_MAP;

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
    public UwaziValue getAnswer() {
        if (TextUtils.isEmpty(locationString)) {
            return null;
        }

        try {
            MyLocation myLocation = parseLocationString();
            return new UwaziValue(new UwaziGeoData("",myLocation.getLatitude(),myLocation.getLongitude()));

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
        UwaziGeoData myLocation=null;
        if (data instanceof UwaziGeoData){
            myLocation  = (UwaziGeoData) data;
        }else {
            LinkedTreeMap<String,Object> locationTreeMap = ((LinkedTreeMap<String,Object>) data);
            String label = String.valueOf(locationTreeMap.get("label"));
            Double longitue = (Double) locationTreeMap.get("lon");
            Double latitude = (Double) locationTreeMap.get("lat");

            myLocation = new UwaziGeoData(label,latitude,longitue);
        }

        locationString = String.format(Locale.ROOT, "%s %s %s %s",
                myLocation.getLat(),
                myLocation.getLon(),
                 "0",
                 "0");

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
        setBinaryData(new UwaziGeoData("",location.getLatitude(),location.getLongitude()));
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

            /*if (!locationGettingPresenter.isGPSProviderEnabled()) {
                onGPSProviderDisabled();
                return;
            }*/

            Activity activity = (Activity) getContext();
            waitingForAData = true;

            MyLocation myLocation = locationString != null ? parseLocationString() : null;

            activity.startActivityForResult(new Intent(getContext(), LocationMapActivity.class)
                            .putExtra(LocationMapActivity.SELECTED_LOCATION, myLocation)
                            .putExtra(LocationMapActivity.CURRENT_LOCATION_ONLY, currentLocationOnly),
                    C.SELECTED_LOCATION
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
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

