package org.horizontal.tella.mobile.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import permissions.dispatcher.PermissionRequest
import org.horizontal.tella.mobile.R

object PermissionUtil {
    @JvmStatic
    fun grantUriPermissions(context: Context, uri: Uri?, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            val resolveInfos = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }
    @JvmStatic
    fun revokeUriPermissions(context: Context, uri: Uri?) {
        try {
            context.revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (ignored: Exception) {
        }
    }
    @JvmStatic
    fun checkPermission(context: Context?, permission: String?): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            permission!!
        ) == PackageManager.PERMISSION_GRANTED
    }
    @JvmStatic
    fun checkPermission(context: Context, permission: String?, infoText: String?): Boolean {
        val activity = context as Activity
        if (!checkPermission(context, permission)) {
            DialogsUtil.showMessageOKCancel(
                context, infoText
            ) { dialog: DialogInterface, which: Int ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            permission!!
                        )
                    ) {
                        activity.requestPermissions(
                            arrayOf<String?>(permission),
                            C.REQUEST_CODE_ASK_PERMISSIONS
                        )
                    } else {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", activity.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                }
                dialog.dismiss()
            }
            return false
        }
        return true
    }

    @JvmStatic
    fun showRationale(context: Context, request: PermissionRequest, message: String?): AlertDialog {
        val activity = context as Activity
        return AlertDialog.Builder(activity)
            .setPositiveButton(activity.resources.getString(R.string.action_ok)) { dialog: DialogInterface?, which: Int -> request.proceed() }
            .setNegativeButton(activity.resources.getString(R.string.action_cancel)) { dialog: DialogInterface?, which: Int -> request.cancel() }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }
}