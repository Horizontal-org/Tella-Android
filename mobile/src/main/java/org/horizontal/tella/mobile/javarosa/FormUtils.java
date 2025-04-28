package org.horizontal.tella.mobile.javarosa;

import android.content.Context;
import android.text.TextUtils;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormSerializingVisitor;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.domain.entity.IErrorBundle;
import org.horizontal.tella.mobile.domain.entity.IErrorCode;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;
import org.horizontal.tella.mobile.util.StringUtils;
import org.horizontal.tella.mobile.util.Util;


public class FormUtils {
    private static final String FORM_METADATA_PROPERTY_DELIMITER = " // ";
    private static final String FORM_METADATA_PROPERTY_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss Z";

    public static boolean doesTheFieldBeginWith(FormEntryPrompt fep, String prefix) {
        String fepName = fep.getIndex().getReference().getNameLast();
        return fepName.startsWith(prefix);
    }

    public static long getFormPayloadSize(@NonNull CollectFormInstance instance) {
        try {
            FormDef formDef = instance.getFormDef();
            FormInstance formInstance = formDef.getInstance();
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();

            ByteArrayPayload payload = (ByteArrayPayload) serializer.createSerializedPayload(formInstance);
            return payload.getLength();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Nullable
    public static String getFormValuesHash(FormDef formDef) {
        FormInstance formInstance = formDef.getInstance();
        XFormSerializingVisitor serializer = new XFormSerializingVisitor();

        try {
            byte[] payload = serializer.serializeInstance(formInstance);

            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(payload);

            return StringUtils.hexString(md.digest());
        } catch (Exception ignored) {
        }

        return null;
    }

    public static String getFormSubmitErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.collect_toast_fail_sending_form);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch (errorBundle.getCode()) {
                case IErrorCode.UNAUTHORIZED:
                    errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                            context.getString(R.string.collect_toast_fail_submission_unauthorized));
                    break;

                case IErrorCode.PAYLOAD_TOO_LARGE_413:
                    errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                            context.getString(R.string.collect_toast_fail_data_too_large));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                    context.getString(R.string.collect_toast_fail_connection_too_weak));
        }

        return errorMessage;
    }

    public static String getFormDefErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.collect_toast_fail_fetch_form_from_server_unknown_error);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            //noinspection SwitchStatementWithTooFewBranches
            switch (errorBundle.getCode()) {
                case IErrorCode.NOT_FOUND:
                    errorMessage = String.format(context.getString(R.string.collect_toast_fail_fetch_form_from_server_display_error),
                            context.getString(R.string.collect_toast_reason_fail_fetch_form_from_server));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                    context.getString(R.string.collect_toast_fail_connection_too_weak));
        }

        return errorMessage;
    }

    static String formatMetadata(Context context, Metadata metadata) {
        List<String> mds = new LinkedList<>();

        mds.add(mdSingleProperty(context, R.string.verification_info_field_filename, metadata.getFileName()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_hash, metadata.getFileHashSHA256()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_file_modified, Util.getDateTimeString(metadata.getTimestamp(), FORM_METADATA_PROPERTY_TIME_FORMAT)));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_manufacturer, metadata.getManufacturer()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_screen_size, metadata.getScreenSize()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_language, metadata.getLanguage()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_locale, metadata.getLocale()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_connection_status, metadata.getNetwork()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_network_type, metadata.getNetworkType()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_wifi_mac, metadata.getWifiMac()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_ipv4, metadata.getIPv4()));
        mds.add(mdSingleProperty(context, R.string.verification_info_field_ipv6, metadata.getIPv6()));
        mds.add(mdLocationProperties(context, metadata.getMyLocation()));
        mds.add(mdListProperty(context, R.string.verification_info_field_cell_towers, metadata.getCells(), true));
        mds.add(mdListProperty(context, R.string.verification_info_wifi, metadata.getWifis(), false));

        return TextUtils.join(FORM_METADATA_PROPERTY_DELIMITER, mds);
    }

    private static String mdSingleProperty(@NonNull Context context, @NonNull String name, @Nullable String value) {
        return String.format(Locale.ROOT, "%s: %s", name, !TextUtils.isEmpty(value) ? value : context.getString(R.string.verification_info_field_metadata_not_available));
    }

    private static String mdSingleProperty(@NonNull Context context, @StringRes int nameResId, @Nullable String value) {
        return mdSingleProperty(context, context.getString(nameResId), value);
    }

    private static String mdListProperty(@NonNull Context context, @StringRes int nameResId, @Nullable List<String> values, boolean group) {
        return mdSingleProperty(context, nameResId, (values != null && values.size() > 0) ?
                String.format(Locale.ROOT, "[%s]", group ? StringUtils.join(", ", values) : TextUtils.join(", ", values)) :
                null);
    }

    private static String mdObjectProperty(@NonNull Context context, @NonNull String objectName, @StringRes int nameResId, @Nullable String value) {
        return mdSingleProperty(context, String.format(Locale.ROOT, "%s.%s", objectName, context.getString(nameResId)), value);
    }

    private static String mdLocationProperties(@NonNull Context context, @Nullable MyLocation location) {
        if (location == null) {
            return mdSingleProperty(context, R.string.verification_collecting_list_item_location, null);
        }

        String objName = context.getString(R.string.verification_collecting_list_item_location);

        List<String> ls = new LinkedList<>();

        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_latitude, Double.toString(location.getLatitude())));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_longitude, Double.toString(location.getLongitude())));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_altitude, location.getAltitude() != null ? location.getAltitude().toString() : null));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_accuracy, location.getAccuracy() != null ? location.getAccuracy().toString() : null));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_location_time, Util.getDateTimeString(location.getTimestamp(), FORM_METADATA_PROPERTY_TIME_FORMAT)));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_location_provider, location.getProvider()));
        ls.add(mdObjectProperty(context, objName, R.string.verification_info_field_location_speed, location.getSpeed() != null ? location.getSpeed().toString() : null));

        return TextUtils.join(FORM_METADATA_PROPERTY_DELIMITER, ls);
    }
}
