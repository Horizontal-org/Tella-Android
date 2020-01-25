package rs.readahead.washington.mobile.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;


public class MetadataUtils {
    public static String getLocale() {
        return Locale.getDefault().getISO3Country();
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

            return String.valueOf(diagonalInches);
        } catch (Exception e) {
            return "-1";
        }
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
        String networkStatus = "";

        final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return networkStatus;
        }

        final NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isConnected()) {
            networkStatus = "WiFi";
        } else if (mobile.isConnected()) {
            networkStatus = getDataType(context);
        } else {
            networkStatus = "No network";
        }

        return networkStatus;
    }

    public static String getNetwork(Context context) {
        return MyApplication.isConnectedToInternet(context) ? "Connected" : "No network";
    }

    public static String getDataType(Context context) {
        String type = "Mobile Data";

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return type;
        }

        switch (tm.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
                type = "Mobile Data CDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                type = "Mobile Data LTE";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                type = "Mobile Data 3G";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                type = "Mobile Data 4G";
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                break;
        }

        return type;
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

    public static String getDeviceID() {
        String deviceId = Preferences.getInstallationId();

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            Preferences.setInstallationId(deviceId);
        }

        return deviceId;
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
