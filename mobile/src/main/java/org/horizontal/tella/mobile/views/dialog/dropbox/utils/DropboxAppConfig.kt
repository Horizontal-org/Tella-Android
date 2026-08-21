package org.horizontal.tella.mobile.views.dialog.dropbox.utils

import org.horizontal.tella.mobile.BuildConfig


class DropboxAppConfig(
    val apiKey: String = BuildConfig.DROPBOX_APP_KEY,
    val clientIdentifier: String = "db-${apiKey}"
)