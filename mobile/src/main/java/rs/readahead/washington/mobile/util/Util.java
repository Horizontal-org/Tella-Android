package rs.readahead.washington.mobile.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;

import rs.readahead.washington.mobile.R;


public class Util {
    private Util() {}

    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    public static long parseLong(@Nullable String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getVideoDuration(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    public static String getShortVideoDuration(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, (seconds % 60));
    }

    public static void startBrowserIntent(Context context, String url) {
        final PackageManager packageManager = context.getPackageManager();

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (browserIntent.resolveActivity(packageManager) != null) {
                context.startActivity(browserIntent);
            }
        } catch (ActivityNotFoundException ignore) {
        }
    }

    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    public static String getDateTimeString() {
        return getDateTimeString(currentTimestamp());
    }

    public static String getDateTimeString(long timestamp) {
        return getDateTimeString(timestamp, "dd-MM-yyyy HH:mm");
    }

    public static String getDateTimeString(long timestamp, @SuppressWarnings("SameParameterValue") String format) {
        try {
            return new SimpleDateFormat(format, Locale.US).format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static String getElapsedTimeFromTimestamp(long timestamp, Context context){
        double interval = (System.currentTimeMillis() - timestamp) / 1000f;

        double years = Math.floor(interval / 31536000); //year = 31536000 sec
        if (years > 0) return context.getString(R.string.Util_ellapsedTime_Year);

        int months = (int) Math.floor(interval / 2592000); //month = 2592000 sec
        if (months > 0) { return context.getResources().getQuantityString(R.plurals.Util_ellapsedTime_Months, months, months);}

        int weeks = (int) Math.floor(interval / 604800); //week = 604800 sec
        if (weeks > 0) { return context.getResources().getQuantityString(R.plurals.Util_ellapsedTime_Weeks, weeks, weeks);}

        int days = (int) Math.floor(interval / 86400);
        if (days > 0) { return context.getResources().getQuantityString(R.plurals.Util_ellapsedTime_Days, days, days);}

        int hours = (int) Math.floor(interval / 3600);
        if (hours > 0) { return context.getResources().getQuantityString(R.plurals.Util_ellapsedTime_Hours, hours, hours);}

        int minutes = (int) Math.floor(interval / 60); //minute = 60 sec
        if (minutes > 0) { return context.getResources().getQuantityString(R.plurals.Util_ellapsedTime_Minutes, minutes, minutes);}

        return context.getString(R.string.Util_ellapsedTime_Seconds);
    }

}
