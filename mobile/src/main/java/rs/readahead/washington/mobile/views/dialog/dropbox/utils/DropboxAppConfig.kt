package rs.readahead.washington.mobile.views.dialog.dropbox.utils

import rs.readahead.washington.mobile.BuildConfig


class DropboxAppConfig(
    val apiKey: String = BuildConfig.DROPBOX_APP_KEY,
    val clientIdentifier: String = "db-${apiKey}"
)