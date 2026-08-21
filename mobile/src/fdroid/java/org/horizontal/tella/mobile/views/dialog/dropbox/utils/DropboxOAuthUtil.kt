package org.horizontal.tella.mobile.views.dialog.dropbox.utils

import android.content.Context

/**
 * Stub implementation of DropboxOAuthUtil for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but all operations
 * do nothing since Dropbox is not available in F-Droid builds.
 */
class DropboxOAuthUtil(
    private val dropboxAppConfig: DropboxAppConfig
) {

    var isAwaitingResult: Boolean = false

    /**
     * Stub implementation - does nothing in F-Droid builds.
     */
    fun startDropboxAuthorization2PKCE(context: Context) {
        isAwaitingResult = false
        // Dropbox is not available in F-Droid builds
    }

    /**
     * Stub implementation - does nothing in F-Droid builds.
     */
    fun startDropboxAuthorizationOAuth2(context: Context) {
        isAwaitingResult = false
        // Dropbox is not available in F-Droid builds
    }
}




