package rs.readahead.washington.mobile.data.entity.mapper;

import android.net.Uri;

import org.javarosa.core.model.FormDef;
import org.javarosa.xform.util.XFormUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rs.readahead.washington.mobile.data.entity.OpenRosaResponseEntity;
import rs.readahead.washington.mobile.data.entity.XFormEntity;
import rs.readahead.washington.mobile.data.http.HttpStatus;
import rs.readahead.washington.mobile.domain.entity.IErrorCode;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OdkForm;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.exception.OpenRosaNegotiationErrorBundle;
import rs.readahead.washington.mobile.domain.exception.OpenRosaSubmissionErrorBundle;


public class OpenRosaDataMapper {
    /*public OpenRosaDocument transform(Response response) throws UnsupportedEncodingException {
        Document document = null;
        ResponseBody body = response.body();

        if (body == null) {
            throw new IllegalArgumentException();
        }

        try {
            InputStreamReader isr = new InputStreamReader(body.byteStream(), "UTF-8");
            document = new Document();

            KXmlParser parser = new KXmlParser();
            parser.setInput(isr);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);

            document.parse(parser);

            isr.close();
            isr = null;

        } catch (Exception e) {
            throw new IllegalArgumentException(); // todo: check this..
        } finally {
            body.close();

        }

        return new OpenRosaDocument(document);
    }*/

    public OdkForm transform(XFormEntity form) {
        OdkForm o = new OdkForm();
        o.setFormID(form.formID);
        o.setName(form.name);
        o.setVersion(notNull(form.version)); // some servers return empty here (Apollo)
        o.setHash(form.hash);
        o.setDescriptionText(form.descriptionText);
        o.setDownloadUrl(form.downloadUrl);
        o.setDescriptionUrl(form.descriptionUrl);
        o.setManifestUrl(form.manifestUrl);

        return o;
    }

    public FormDef transform(ResponseBody responseBody) {
        if (responseBody == null) return null;

        try {
            return XFormUtils.getFormFromInputStream(responseBody.byteStream());
        } finally {
            responseBody.close();
        }
    }

    public NegotiatedCollectServer transform(CollectServer server, Response response) throws Exception {
        if (! response.isSuccessful()) {
            throw new OpenRosaNegotiationErrorBundle(IErrorCode.UNAUTHORIZED);
        }

        if (response.code() != HttpStatus.NO_CONTENT_204) {
            throw new OpenRosaNegotiationErrorBundle(IErrorCode.ORN_NOT_204_RESPONSE);
        }

        NegotiatedCollectServer newServer = new NegotiatedCollectServer();
        newServer.setId(server.getId());
        newServer.setUsername(server.getUsername());
        newServer.setPassword(server.getPassword());

        // maybe fix url
        String location = response.headers().get("location");
        if (location != null) {
            Uri oldUri = Uri.parse(server.getUrl());
            Uri newUri = Uri.parse(location);

            if (oldUri == null) {
                throw new OpenRosaNegotiationErrorBundle(IErrorCode.ORN_BAD_HOST);
            }

            if (! "https".equalsIgnoreCase(newUri.getScheme())) {
                throw new OpenRosaNegotiationErrorBundle(IErrorCode.ORN_NOT_HTTPS_HOST);
            }

            if (! (oldUri.getHost() != null && oldUri.getHost().equals(newUri.getHost()))) {
                throw new OpenRosaNegotiationErrorBundle(IErrorCode.ORN_BAD_LOCATION_HEADER);
            }

            newServer.setUrl(location);
            newServer.setUrlNegotiated(true);
        } else {
            newServer.setUrl(server.getUrl());
        }

        // Set OpenRosa headers data
        String openRosaVersion = response.headers().get("x-openrosa-version");
        if (openRosaVersion != null) {
            newServer.setOpenRosaVersion(openRosaVersion);
            newServer.setOpenRosa(true); // todo: or anyway set this to true?
        }

        String openRosaAcceptContentLength = response.headers().get("x-openrosa-accept-content-length");
        if (openRosaVersion != null) {
            try {
                newServer.setOpenRosaAcceptContentLength(Integer.parseInt(openRosaAcceptContentLength));
            } catch (NumberFormatException ignored) {}
        }

        return newServer;
    }

    public OpenRosaResponse transform(Response<OpenRosaResponseEntity> retrofitResponse) throws Exception {
        if (retrofitResponse.body() == null) { // no body if something went wrong
            throw new OpenRosaSubmissionErrorBundle(retrofitResponse.code());
        }

        OpenRosaResponse response = new OpenRosaResponse();
        response.setMessages(transform(retrofitResponse.body().messages));
        response.setStatusCode(retrofitResponse.code());
        return response;
    }

    public OpenRosaPartResponse transform(Response<OpenRosaResponseEntity> retrofitResponse, String name) throws Exception {
        if (retrofitResponse.body() == null) { // no body if something went wrong
            throw new OpenRosaSubmissionErrorBundle(retrofitResponse.code());
        }

        OpenRosaPartResponse response = new OpenRosaPartResponse(name);
        response.setMessages(transform(retrofitResponse.body().messages));
        response.setStatusCode(retrofitResponse.code());
        return response;
    }

    private List<OpenRosaResponse.Message> transform(List<OpenRosaResponseEntity.MessageEntity> entities) {
        List<OpenRosaResponse.Message> messages = new ArrayList<>(entities.size());

        for (OpenRosaResponseEntity.MessageEntity entity: entities) {
            OpenRosaResponse.Message message = new OpenRosaResponse.Message();
            message.setNature(entity.nature);
            message.setText(entity.text);
            messages.add(message);
        }

        return messages;
    }

    private String notNull(String str) {
        return str != null ? str : "";
    }
}
