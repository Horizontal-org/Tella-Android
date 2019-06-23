package rs.readahead.washington.mobile.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.TextUtils;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import timber.log.Timber;


public class OpenPassword extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, final Intent intent) {
        if (! Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            return;
        }

        if (! Preferences.isSecretModeActive()) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        String phoneNumber = bundle.getString(Intent.EXTRA_PHONE_NUMBER);
        if (TextUtils.isEmpty(phoneNumber)) {
            return;
        }

        // remove non-digits from code
        String password = phoneNumber.replaceAll("[^\\d]", "");

        if (! password.equals(Preferences.getSecretPassword())) {
            return;
        }

        setResultData(null);

        deleteNumber(context, phoneNumber);

        MyApplication.startMainActivity(context);
    }

    private void deleteNumber(Context context, String phoneNumber) {
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    null,
                    CallLog.Calls.NUMBER + " = ?",
                    new String[] {phoneNumber},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idOfRowToDelete = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                    context.getContentResolver().delete(
                            Uri.withAppendedPath(CallLog.Calls.CONTENT_URI, Integer.toString(idOfRowToDelete)),
                            null,
                            null);
                } while (cursor.moveToNext());
            }
        } catch (SecurityException ex) {
           Timber.d(ex, getClass().getName());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
