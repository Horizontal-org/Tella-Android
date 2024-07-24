/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2020 Infomaniak Network SA
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2020 Kilian Périsset <kilian.perisset@infomaniak.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Harikrishnan Rajan <rhari991@gmail.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Lennart Rosam <lennart@familie-rosam.de>
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package rs.readahead.washington.mobile.util.operations;


import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.net.IDN;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A helper class for UI/display related operations.
 */
public final class DisplayUtils {
    private static final String TAG = DisplayUtils.class.getSimpleName();

    private static final String[] sizeSuffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final int[] sizeScales = {0, 0, 1, 1, 1, 2, 2, 2, 2};
    private static final String MIME_TYPE_UNKNOWN = "Unknown type";

    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String TWITTER_HANDLE_PREFIX = "@";
    private static final int MIMETYPE_PARTS_COUNT = 2;
    private static final int BYTE_SIZE_DIVIDER = 1024;
    private static final double BYTE_SIZE_DIVIDER_DOUBLE = 1024.0;
    private static final int DATE_TIME_PARTS_SIZE = 2;

    public static final String MONTH_YEAR_PATTERN = "MMMM yyyy";
    public static final String MONTH_PATTERN = "MMMM";
    public static final String YEAR_PATTERN = "yyyy";
    public static final int SVG_SIZE = 512;

    private static Map<String, String> mimeType2HumanReadable;

    static {
        mimeType2HumanReadable = new HashMap<>();
        // images
        mimeType2HumanReadable.put("image/jpeg", "JPEG image");
        mimeType2HumanReadable.put("image/jpg", "JPEG image");
        mimeType2HumanReadable.put("image/png", "PNG image");
        mimeType2HumanReadable.put("image/bmp", "Bitmap image");
        mimeType2HumanReadable.put("image/gif", "GIF image");
        mimeType2HumanReadable.put("image/svg+xml", "JPEG image");
        mimeType2HumanReadable.put("image/tiff", "TIFF image");
        // music
        mimeType2HumanReadable.put("audio/mpeg", "MP3 music file");
        mimeType2HumanReadable.put("application/ogg", "OGG music file");
    }

    private DisplayUtils() {
        // utility class -> private constructor
    }

    /**
     * Converts MIME types like "image/jpg" to more end user friendly output
     * like "JPG image".
     *
     * @param mimetype MIME type to convert
     * @return A human friendly version of the MIME type, {@link #MIME_TYPE_UNKNOWN} if it can't be converted
     */
    public static String convertMIMEtoPrettyPrint(String mimetype) {
        final String humanReadableMime = mimeType2HumanReadable.get(mimetype);
        if (humanReadableMime != null) {
            return humanReadableMime;
        }
        if (mimetype.split("/").length >= MIMETYPE_PARTS_COUNT) {
            return mimetype.split("/")[1].toUpperCase(Locale.getDefault()) + " file";
        }
        return MIME_TYPE_UNKNOWN;
    }

    /**
     * Converts Unix time to human readable format
     *
     * @param milliseconds that have passed since 01/01/1970
     * @return The human readable time for the users locale
     */
    public static String unixTimeToHumanReadable(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(date);
    }

    /**
     * beautifies a given URL by removing any http/https protocol prefix.
     *
     * @param url to be beautified url
     * @return beautified url
     */
    public static String beautifyURL(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        if (url.length() >= 7 && HTTP_PROTOCOL.equalsIgnoreCase(url.substring(0, 7))) {
            return url.substring(HTTP_PROTOCOL.length()).trim();
        }

        if (url.length() >= 8 && HTTPS_PROTOCOL.equalsIgnoreCase(url.substring(0, 8))) {
            return url.substring(HTTPS_PROTOCOL.length()).trim();
        }

        return url.trim();
    }

    /**
     * beautifies a given twitter handle by prefixing it with an @ in case it is missing.
     *
     * @param handle to be beautified twitter handle
     * @return beautified twitter handle
     */
    public static String beautifyTwitterHandle(@Nullable String handle) {
        if (handle != null) {
            String trimmedHandle = handle.trim();

            if (TextUtils.isEmpty(trimmedHandle)) {
                return "";
            }

            if (trimmedHandle.startsWith(TWITTER_HANDLE_PREFIX)) {
                return trimmedHandle;
            } else {
                return TWITTER_HANDLE_PREFIX + trimmedHandle;
            }
        } else {
            return "";
        }
    }

    /**
     * Converts an internationalized domain name (IDN) in an URL to and from ASCII/Unicode.
     *
     * @param url the URL where the domain name should be converted
     * @param toASCII if true converts from Unicode to ASCII, if false converts from ASCII to Unicode
     * @return the URL containing the converted domain name
     */
    public static String convertIdn(String url, boolean toASCII) {

        String urlNoDots = url;
        String dots = "";
        while (urlNoDots.length() > 0 && urlNoDots.charAt(0) == '.') {
            urlNoDots = url.substring(1);
            dots = dots + ".";
        }

        // Find host name after '//' or '@'
        int hostStart = 0;
        if (urlNoDots.contains("//")) {
            hostStart = url.indexOf("//") + "//".length();
        } else if (url.contains("@")) {
            hostStart = url.indexOf('@') + "@".length();
        }

        int hostEnd = url.substring(hostStart).indexOf('/');
        // Handle URL which doesn't have a path (path is implicitly '/')
        hostEnd = hostEnd == -1 ? urlNoDots.length() : hostStart + hostEnd;

        String host = urlNoDots.substring(hostStart, hostEnd);
        host = toASCII ? IDN.toASCII(host) : IDN.toUnicode(host);

        return dots + urlNoDots.substring(0, hostStart) + host + urlNoDots.substring(hostEnd);
    }


}
