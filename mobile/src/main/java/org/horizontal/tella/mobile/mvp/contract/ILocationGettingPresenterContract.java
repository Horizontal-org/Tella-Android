package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;
import android.location.Location;


public class ILocationGettingPresenterContract {
    public interface IView {
        void onGettingLocationStart();
        void onGettingLocationEnd();
        void onLocationSuccess(Location location);
        void onNoLocationPermissions();
        void onGPSProviderDisabled();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void startGettingLocation(boolean useLastKnownLocation);
        void stopGettingLocation();
        boolean isLocationPermissionAllowed();
        boolean isGPSProviderEnabled();
    }
}
