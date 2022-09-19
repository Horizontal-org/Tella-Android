package rs.readahead.washington.mobile.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.model.xform.XPathReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import rs.readahead.washington.mobile.data.entity.XFormEntity;
import rs.readahead.washington.mobile.data.entity.mapper.OpenRosaDataMapper;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.domain.entity.IProgressListener;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.StringUtils;


public class OpenRosaRepository implements IOpenRosaRepository {
    private static final String FORM_LIST_PATH = "formList";
    private static final String SUBMISSION_PATH = "submission";


    @Override
    public Single<ListFormResult> formList(final CollectServer server) {
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().formList(null, StringUtils.append('/', server.getUrl(), FORM_LIST_PATH))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(formsEntity -> {
                    List<CollectForm> forms = new ArrayList<>();
                    OpenRosaDataMapper mapper = new OpenRosaDataMapper();

                    for (XFormEntity form: formsEntity.xforms) {
                        forms.add(new CollectForm(server.getId(), mapper.transform(form)));
                    }

                    ListFormResult listFormResult = new ListFormResult();
                    listFormResult.setForms(forms);

                    return listFormResult;
                })
                .onErrorResumeNext(throwable -> {
                    ListFormResult listFormResult = new ListFormResult();
                    ErrorBundle errorBundle = new ErrorBundle(throwable);
                    errorBundle.setServerId(server.getId());
                    errorBundle.setServerName(server.getName());
                    listFormResult.setErrors(Collections.singletonList(errorBundle));

                    return Single.just(listFormResult);
                });
    }

    @Override
    public Single<FormDef> getFormDef(CollectServer server, CollectForm form) {
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().getFormDef(null, form.getForm().getDownloadUrl())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> new OpenRosaDataMapper().transform(response))
                .onErrorResumeNext(throwable -> {
                    XErrorBundle errorBundle = new XErrorBundle(throwable);
                    return Single.error(errorBundle);
                });
    }

    @Override
    public Single<NegotiatedCollectServer> submitFormNegotiate(final CollectServer server) {
        // todo: InstanceColumns.SUBMISSION_URI? url in form
        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().submitFormNegotiate(null, StringUtils.append('/', server.getUrl(), SUBMISSION_PATH))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> new OpenRosaDataMapper().transform(server, response))
                .onErrorResumeNext(Single::error);
    }

    @Override
    public Single<OpenRosaResponse> submitForm(Context context, NegotiatedCollectServer server, CollectFormInstance instance) {
        Map<String, RequestBody> parts = new LinkedHashMap<>();

        try {
            FormDef formDef = instance.getFormDef();
            FormInstance formInstance = formDef.getInstance();
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();
            ByteArrayPayload payload = (ByteArrayPayload) serializer.createSerializedPayload(formInstance, getSubmissionDataReference(formDef));

            parts.put(getPartKey(C.OPEN_ROSA_XML_PART_NAME, C.OPEN_ROSA_XML_PART_NAME + ".xml"),
                    RequestBody.create(MediaType.parse("text/xml"), IOUtils.toByteArray(payload.getPayloadStream())));

            // add attachment parts
            for (FormMediaFile attachment: instance.getWidgetMediaFiles()) {
                if (!attachment.uploading || attachment.status == FormMediaFileStatus.SUBMITTED) {
                    continue;
                }

                String filename = attachment.name;
                parts.put(String.format(Locale.ROOT, "%s\"; filename=\"%s", filename, filename), // again - wtf, OkHttp3 :)
                        new MediaFileRequestBody(attachment));
            }
        } catch (IOException e) {
            return Single.error(e);
        }

        String url = server.isUrlNegotiated() ? server.getUrl() : StringUtils.append('/', server.getUrl(), SUBMISSION_PATH);

        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().submitForm(null, url, parts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> new OpenRosaDataMapper().transform(response))
                .onErrorResumeNext(Single::error);
    }

    @Override
    public Single<OpenRosaPartResponse> submitFormGranular(
            @NonNull Context context,
            @NonNull NegotiatedCollectServer server,
            @NonNull CollectFormInstance instance,
            @Nullable FormMediaFile attachment,
            @Nullable IProgressListener progressListener) {
        Map<String, RequestBody> parts = new LinkedHashMap<>();

        try {
            FormDef formDef = instance.getFormDef();
            FormInstance formInstance = formDef.getInstance();
            XFormSerializingVisitor serializer = new XFormSerializingVisitor();
            ByteArrayPayload payload = (ByteArrayPayload) serializer.createSerializedPayload(formInstance, getSubmissionDataReference(formDef));

            parts.put(getPartKey(C.OPEN_ROSA_XML_PART_NAME, C.OPEN_ROSA_XML_PART_NAME + ".xml"),
                    RequestBody.create(MediaType.parse("text/xml"), IOUtils.toByteArray(payload.getPayloadStream())));

            if (attachment != null) {
                parts.put(getPartKey(attachment.getPartName(), attachment.id),
                        new MediaFileRequestBody(attachment, progressListener));
            }
        } catch (IOException e) {
            return Single.error(e);
        }

        final String url = server.isUrlNegotiated() ? server.getUrl() : StringUtils.append('/', server.getUrl(), SUBMISSION_PATH);
        final String partName = attachment != null ? attachment.getPartName() : C.OPEN_ROSA_XML_PART_NAME;

        return OpenRosaService.newInstance(server.getUsername(), server.getPassword())
                .getServices().submitForm(null, url, parts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> new OpenRosaDataMapper().transform(response, partName))
                .onErrorResumeNext(Single::error);
    }

    private String getPartKey(String partName, String filename) {
        return String.format(Locale.ROOT, "%s\"; filename=\"%s", partName, filename); // wtf, OkHttp3 :)
    }

    // todo: submission profile for SMS? check it out
    private IDataReference getSubmissionDataReference(FormDef formDef) {
        // Determine the information about the submission...
        SubmissionProfile p = formDef.getSubmissionProfile();
        if (p == null || p.getRef() == null) {
            return new XPathReference("/");
        } else {
            return p.getRef();
        }
    }
}
