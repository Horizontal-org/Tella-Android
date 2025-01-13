package rs.readahead.washington.mobile.views.fragment.googledrive.di

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DriveServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDriveService(email: String): Drive {
        val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE)
        ).apply {
            selectedAccountName = email
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            googleAccountCredential
        ).setApplicationName("Tella").build()
    }

}