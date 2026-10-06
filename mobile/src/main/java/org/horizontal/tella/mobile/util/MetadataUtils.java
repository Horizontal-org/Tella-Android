package org.horizontal.tella.mobile.util;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import org.horizontal.tella.mobile.MyApplication;


public class MetadataUtils {
    public static String getLocale() {
        String country = countryCode(getDeviceLocale());
        if (!country.isEmpty()) {
            return country;
        }
        return countryCode(Locale.getDefault());
    }

    private static Locale getDeviceLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        }
        //noinspection deprecation
        return Resources.getSystem().getConfiguration().locale;
    }

    private static String countryCode(Locale locale) {
        if (locale == null) {
            return "";
        }
        try {
            String iso3 = locale.getISO3Country();
            if (iso3 != null && !iso3.isEmpty()) {
                return iso3;
            }
        } catch (MissingResourceException ignored) {
        }
        String iso2 = locale.getCountry();
        return iso2 != null ? iso2 : "";
    }

    public static String getLanguage() {
        return Locale.getDefault().getDisplayLanguage();
    }

    public static String getScreenSize(Context context) {
        try {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

            float yInches = displayMetrics.heightPixels / displayMetrics.ydpi;
            float xInches = displayMetrics.widthPixels / displayMetrics.xdpi;

            double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);

            return formatScreenSize(String.valueOf(diagonalInches));
        } catch (Exception e) {
            return "-1";
        }
    }

    public static String formatScreenSize(String screenSize) {
        if (screenSize == null || screenSize.isEmpty() || "-1".equals(screenSize)) {
            return screenSize;
        }
        if (screenSize.endsWith("\"") || screenSize.endsWith("\u2033") || screenSize.endsWith("\u201D")) {
            return screenSize;
        }
        return screenSize + "\"";
    }

    public static String getManufacturer() {
        return StringUtils.capitalize(Build.MANUFACTURER, Locale.ROOT);
    }

    public static String getHardware() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return StringUtils.capitalize(model, Locale.ROOT);
        } else {
            return StringUtils.capitalize(manufacturer, Locale.ROOT) + " " + model;
        }
    }

    public static String getNetworkType(Context context) {
        final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return "";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return "No network";
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "No network";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return getDataType(context);
            }
            return "No network";
        }

        return getNetworkTypeLegacy(cm, context);
    }

    @SuppressWarnings("deprecation")
    private static String getNetworkTypeLegacy(ConnectivityManager cm, Context context) {
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi != null && wifi.isConnected()) {
            return "WiFi";
        }
        if (mobile != null && mobile.isConnected()) {
            return getDataType(context);
        }
        return "No network";
    }

    public static String getNetwork(Context context) {
        return MyApplication.isConnectedToInternet(context) ? "Connected" : "No network";
    }

    public static String getDataType(Context context) {
        final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return "";
        }

        if (!isMobileConnected(cm)) {
            return "";
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return "Other Mobile Data type";
        }

        try {
            switch (tm.getNetworkType()) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return "Mobile Data CDMA";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "Mobile Data LTE";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "Mobile Data 5G";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return "Mobile Data 3G";
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "Mobile Data 4G";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "Mobile Data GPRS";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "Mobile Data EDGE";
                default:
                    return "Other Mobile Data type";
            }
        } catch (Exception ignored) {
            return "Other Mobile Data type";
        }
    }

    private static boolean isMobileConnected(ConnectivityManager cm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        return isMobileConnectedLegacy(cm);
    }

    @SuppressWarnings("deprecation")
    private static boolean isMobileConnectedLegacy(ConnectivityManager cm) {
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobile != null && mobile.isConnected();
    }

    public static String getIPv6() {
        return getIPAddresses(false);
    }

    public static String getIPv4() {
        return getIPAddresses(true);
    }

    public static String getWifiMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface nif: all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) {
                    continue;
                }

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF)).append(":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }

                return res1.toString();
            }
        } catch (Exception ignored) {
        }

        return "02:00:00:00:00:00";
    }

    public static String getDeviceID(Context context) {
        if (context == null) {
            return "";
        }
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        return androidId != null ? androidId : "";
    }

    private static String getIPAddresses(boolean IPv4) {
        StringBuilder sb = new StringBuilder();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface iface : interfaces) {
                List<InetAddress> addresses = Collections.list(iface.getInetAddresses());

                for (InetAddress address: addresses) {
                    if (address.isLoopbackAddress()) {
                        continue;
                    }

                    String addressStr = address.getHostAddress().toUpperCase();
                    boolean isIPv4 = address instanceof Inet4Address;

                    if (IPv4) {
                        if (isIPv4) {
                            sb.append(addressStr).append(' ');
                        }
                    } else {
                        if (!isIPv4) {
                            int delim = addressStr.indexOf('%');
                            sb.append(delim < 0 ? addressStr : addressStr.substring(0, delim)).append(' ');
                        }
                    }

                }
            }
        } catch (Exception ignored) {
        }

        return sb.toString();
    }
}
