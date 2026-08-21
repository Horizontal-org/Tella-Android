package org.horizontal.tella.mobile.util;

import android.location.Location;
import androidx.annotation.NonNull;


public class LocationUtil {
    private static final int TWO_MINUTES = 1000 * 60 * 2;


    public static String getLocationData(Location location){
        return C.GOOGLE_MAPS_TEST + location.getLatitude() +
                "," + location.getLongitude();
    }

    // Taking android example of android "location strategy" as good enough starting point..
    public static boolean isBetterLocation(@NonNull Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) { // todo: this one should be considered
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }

        return false;
    }

    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }

        return provider1.equals(provider2);
    }

    private static String convertCoordinate(double coordinate, boolean isLatitude){
        String[] pString = isLatitude ? new String[]{"N", "S"} :  new String[]{"E", "W"};
        String[] separated = Location.convert(coordinate,Location.FORMAT_SECONDS).split(":");
        int index = ((int) coordinate) < 0 ? 1 : 0;

        return separated[0] +"%C2%B0" +  separated[1] + "'" + separated[2] + "%22" +  pString[index];
    }

    public static String printCoordinate(double coordinate, boolean isLatitude){
        String[] pString = isLatitude ? new String[]{"N", "S"} : new String[]{"E", "W"};
        String[] separated = Location.convert(coordinate, Location.FORMAT_SECONDS).split(":");
        int index = ((int) coordinate) < 0 ? 1 : 0;

        return separated[0] + (char) 0x00B0 + " " + separated[1] + "' " + separated[2] + "\" " +  pString[index];
    }

//    public static String getLocationData(Location location){
//        return C.GOOGLE_MAPS_TEST + convertCoordinate(location.getLatitude(), true) +
//                "+" + convertCoordinate(location.getLongitude(), false) +
//                "/@" + location.getLatitude() + "," + location.getLongitude();
//    }
}
