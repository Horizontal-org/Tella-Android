package org.horizontal.tella.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.hzontal.tella_vault.MyLocation;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Locale;



/**
 * Based on ODK GeoPointWidget.
 */
@SuppressLint("ViewConstructor")
public class GeoPointHiddenWidget extends QuestionWidget {

    private String locationString;

    public GeoPointHiddenWidget(Context context, @NonNull FormEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

        locationString = formEntryPrompt.getAnswerText();

        this.setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public IAnswerData getAnswer() {
        if (TextUtils.isEmpty(locationString)) {
            return null;
        }

        try {
            MyLocation myLocation = parseLocationString();

            return new GeoPointData(new double[]{
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

        return locationString;
    }

    @Nullable
    @Override
    public String getBinaryName() {
        return locationString;
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
}
