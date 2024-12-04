/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package rs.readahead.washington.mobile.util.operations;

/**
 * Class containing the mime types.
 */
public final class MimeType {
    public static final String DIRECTORY = "DIR";
    public static final String WEBDAV_FOLDER = "httpd/unix-directory";
    public static final String FILE = "application/octet-stream";
    public static final String PDF = "application/pdf";

    private MimeType() {
        // No instance
    }
}
