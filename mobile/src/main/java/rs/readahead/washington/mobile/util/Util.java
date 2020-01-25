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
}
