package rs.readahead.washington.mobile.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import permissions.dispatcher.PermissionRequest;
import rs.readahead.washington.mobile.R;


public class PermissionUtil {
    public static void grantUriPermissions(Context context, Uri uri, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo: resolveInfos) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }

    public static void revokeUriPermissions(Context context, Uri uri) {
        try {
            context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
    }

    public static boolean checkPermission(final Context context, final String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkPermission(final Context context, final String permission, String infoText) {
        final Activity activity = (Activity) context;

        if (! checkPermission(context, permission)) {
            DialogsUtil.showMessageOKCancel(context, infoText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                                    activity.requestPermissions(new String[]{permission},
                                            C.REQUEST_CODE_ASK_PERMISSIONS);
                                } else {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                    intent.setData(uri);
                                    context.startActivity(intent);
                                }
                            }
                            dialog.dismiss();
                        }
                    });

            return false;
        }

        return true;
    }

    public static AlertDialog showRationale(final Context context, final PermissionRequest request, final String message) {
        final Activity activity = (Activity) context;

        return new AlertDialog.Builder(activity)
                .setPositiveButton(activity.getResources().getString(R.string.action_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(activity.getResources().getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(message)
                .show();
    }
}
