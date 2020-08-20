package rs.readahead.washington.mobile.javarosa;

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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.IErrorCode;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.MyLocation;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;


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

    public static String getFormSubmitSuccessMessage(Context context, OpenRosaResponse response) {
        List<String> texts = new ArrayList<>();
        for (OpenRosaResponse.Message msg : response.getMessages()) {
            if (!TextUtils.isEmpty(msg.getText())) {
                texts.add(msg.getText());
            }
        }

        String messages, successMessage;
        boolean hasMessages = texts.size() > 0;
        messages = hasMessages ? TextUtils.join("; ", texts) : "";

        switch (response.getStatusCode()) {
            case OpenRosaResponse.StatusCode.FORM_RECEIVED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.collect_toast_server_response_form_received) + messages;
                } else {
                    successMessage = context.getString(R.string.collect_toast_server_reply_form_received_no_detail);
                }
                break;

            case OpenRosaResponse.StatusCode.ACCEPTED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.collect_toast_server_reply_form_accepted_with_detail) + messages;
                } else {
                    successMessage = context.getString(R.string.collect_toast_server_reply_form_accepted_no_detail);
                }
                break;

            case OpenRosaResponse.StatusCode.UNUSED:
            default:
                successMessage = String.format(Locale.US,
                        context.getString(R.string.settings_docu_toast_server_not_odk),
                        response.getStatusCode());
                break;
        }

        return successMessage;
    }

    public static String getFormSubmitErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.collect_toast_fail_sending_form);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch (errorBundle.getCode()) {
                case IErrorCode.UNAUTHORIZED:
                    errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                            context.getString(R.string.ra_unauthorized));
                    break;

                case IErrorCode.PAYLOAD_TOO_LARGE_413:
                    errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                            context.getString(R.string.ra_submitted_data_too_large));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                    context.getString(R.string.ra_internet_conection_too_weak));
        }

        return errorMessage;
    }

    public static String getFormDefErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.ra_error_get_form_def);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            //noinspection SwitchStatementWithTooFewBranches
            switch (errorBundle.getCode()) {
                case IErrorCode.NOT_FOUND:
                    errorMessage = String.format(context.getString(R.string.ra_error_get_form_def_tmp),
                            context.getString(R.string.ra_not_found));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.collect_toast_fail_form_submission_generic),
                    context.getString(R.string.ra_internet_conection_too_weak));
        }

        return errorMessage;
    }

    static String formatMetadata(Context context, Metadata metadata) {
        List<String> mds = new LinkedList<>();

        mds.add(mdSingleProperty(context, R.string.filename, metadata.getFileName()));
        mds.add(mdSingleProperty(context, R.string.filehash, metadata.getFileHashSHA256()));
        mds.add(mdSingleProperty(context, R.string.file_modified, Util.getDateTimeString(metadata.getTimestamp(), FORM_METADATA_PROPERTY_TIME_FORMAT)));
        mds.add(mdSingleProperty(context, R.string.manufacturer, metadata.getManufacturer()));
        mds.add(mdSingleProperty(context, R.string.screen_size, metadata.getScreenSize()));
        mds.add(mdSingleProperty(context, R.string.language, metadata.getLanguage()));
        mds.add(mdSingleProperty(context, R.string.locale, metadata.getLocale()));
        mds.add(mdSingleProperty(context, R.string.connection_status, metadata.getNetwork()));
        mds.add(mdSingleProperty(context, R.string.network_type, metadata.getNetworkType()));
        mds.add(mdSingleProperty(context, R.string.wifi_mac, metadata.getWifiMac()));
        mds.add(mdSingleProperty(context, R.string.ipv4, metadata.getIPv4()));
        mds.add(mdSingleProperty(context, R.string.ipv6, metadata.getIPv6()));
        mds.add(mdLocationProperties(context, metadata.getMyLocation()));
        mds.add(mdListProperty(context, R.string.cell_info, metadata.getCells(), true));
        mds.add(mdListProperty(context, R.string.ra_wifi_info, metadata.getWifis(), false));

        return TextUtils.join(FORM_METADATA_PROPERTY_DELIMITER, mds);
    }

    private static String mdSingleProperty(@NonNull Context context, @NonNull String name, @Nullable String value) {
        return String.format(Locale.ROOT, "%s: %s", name, !TextUtils.isEmpty(value) ? value : context.getString(R.string.not_available));
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
            return mdSingleProperty(context, R.string.location, null);
        }

        String objName = context.getString(R.string.location);

        List<String> ls = new LinkedList<>();

        ls.add(mdObjectProperty(context, objName, R.string.latitude, Double.toString(location.getLatitude())));
        ls.add(mdObjectProperty(context, objName, R.string.longitude, Double.toString(location.getLongitude())));
        ls.add(mdObjectProperty(context, objName, R.string.altitude, location.getAltitude() != null ? location.getAltitude().toString() : null));
        ls.add(mdObjectProperty(context, objName, R.string.accuracy, location.getAccuracy() != null ? location.getAccuracy().toString() : null));
        ls.add(mdObjectProperty(context, objName, R.string.time, Util.getDateTimeString(location.getTimestamp(), FORM_METADATA_PROPERTY_TIME_FORMAT)));
        ls.add(mdObjectProperty(context, objName, R.string.location_provider, location.getProvider()));
        ls.add(mdObjectProperty(context, objName, R.string.location_speed, location.getSpeed() != null ? location.getSpeed().toString() : null));

        return TextUtils.join(FORM_METADATA_PROPERTY_DELIMITER, ls);
    }
}
