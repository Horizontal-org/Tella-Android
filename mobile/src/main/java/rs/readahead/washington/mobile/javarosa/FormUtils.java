package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.List;
import java.util.Locale;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.IErrorCode;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.util.StringUtils;


public class FormUtils {

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
        for (OpenRosaResponse.Message msg: response.getMessages()) {
            if (! TextUtils.isEmpty(msg.getText())) {
                texts.add(msg.getText());
            }
        }

        String messages, successMessage;
        boolean hasMessages = texts.size() > 0;
        messages = hasMessages ? TextUtils.join("; ", texts) : "";

        switch(response.getStatusCode()) {
            case OpenRosaResponse.StatusCode.FORM_RECEIVED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.ra_form_received_reply) + messages;
                } else {
                    successMessage = context.getString(R.string.ra_form_received_no_reply);
                }
                break;

            case OpenRosaResponse.StatusCode.ACCEPTED:
                if (hasMessages) {
                    successMessage = context.getString(R.string.ra_form_accepted_reply) + messages;
                } else {
                    successMessage = context.getString(R.string.ra_form_accepted_no_reply);
                }
                break;

            case OpenRosaResponse.StatusCode.UNUSED:
            default:
                successMessage = String.format(Locale.US,
                        context.getString(R.string.ra_form_unused_reply),
                        response.getStatusCode());
                break;
        }

        return successMessage;
    }

    public static String getFormSubmitErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.ra_error_submitting_form);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch(errorBundle.getCode()) {
                case IErrorCode.UNAUTHORIZED:
                    errorMessage = String.format(context.getString(R.string.ra_error_submitting_form_tmp),
                            context.getString(R.string.ra_unauthorized));
                    break;

                case IErrorCode.PAYLOAD_TOO_LARGE_413:
                    errorMessage = String.format(context.getString(R.string.ra_error_submitting_form_tmp),
                            context.getString(R.string.ra_submitted_data_too_large));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.ra_error_submitting_form_tmp),
                    context.getString(R.string.ra_internet_conection_too_weak));
        }

        return errorMessage;
    }

    public static String getFormDefErrorMessage(Context context, Throwable error) {
        String errorMessage = context.getString(R.string.ra_error_get_form_def);

        if (error instanceof IErrorBundle) {
            IErrorBundle errorBundle = (IErrorBundle) error;

            switch(errorBundle.getCode()) {
                case IErrorCode.NOT_FOUND:
                    errorMessage = String.format(context.getString(R.string.ra_error_get_form_def_tmp),
                            context.getString(R.string.ra_not_found));
                    break;
            }
        } else if (error instanceof SocketTimeoutException) {
            errorMessage = String.format(context.getString(R.string.ra_error_submitting_form_tmp),
                    context.getString(R.string.ra_internet_conection_too_weak));
        }

        return errorMessage;
    }
}
