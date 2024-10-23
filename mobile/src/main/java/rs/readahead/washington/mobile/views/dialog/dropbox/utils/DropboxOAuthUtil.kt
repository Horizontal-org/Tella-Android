package rs.readahead.washington.mobile.views.dialog.dropbox.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth

class DropboxOAuthUtil(
    private val dropboxAppConfig: DropboxAppConfig
) {

    var isAwaitingResult: Boolean = false

    /**
     * Starts the Dropbox OAuth process by launching the Dropbox official app or web
     * browser if dropbox official app is not available. In browser flow, normally user needs to
     * sign in.
     *
     * Because mobile apps need to keep Dropbox secrets in their binaries we need to use PKCE.
     * Read more about this here: https://dropbox.tech/developers/pkce--what-and-why-
     **/
    fun startDropboxAuthorization2PKCE(context: Context) {
        val requestConfig = DbxRequestConfig(dropboxAppConfig.clientIdentifier)

        // The scope's your app will need from Dropbox
        // Read more about Scopes here: https://developers.dropbox.com/oauth-guide#dropbox-api-permissions
        val scopes = listOf(
            "account_info.read",
            "files.content.write",
            "files.content.read",
            "sharing.read"
        )
        Auth.startOAuth2PKCE(context, dropboxAppConfig.apiKey, requestConfig, scopes)
        isAwaitingResult = true
    }

    /**
     * Starts the Dropbox OAuth process by launching the Dropbox official app or web
     * browser if dropbox official app is not available. In browser flow, normally user needs to
     * sign in.
     *
     * Because mobile apps need to keep Dropbox secrets in their binaries we need to use PKCE.
     * Read more about this here: https://dropbox.tech/developers/pkce--what-and-why-
     **/
    fun startDropboxAuthorizationOAuth2(context: Context) {
        Auth.startOAuth2Authentication(context, dropboxAppConfig.apiKey)
        isAwaitingResult = true
    }

}