package org.horizontal.tella.mobile.data.openrosa;

import java.util.Map;

import io.reactivex.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Url;
import org.horizontal.tella.mobile.data.entity.OpenRosaResponseEntity;
import org.horizontal.tella.mobile.data.entity.XFormsEntity;


public interface IOpenRosaApi {
    @GET/*("formList")*/
    Single<XFormsEntity> formList(
            @Header("Authorization") String authorization,
            @Url String url
    );

    @GET
    Single<ResponseBody> getFormDef(
            @Header("Authorization") String authorization,
            @Url String baseUrl
    );

    @HEAD/*("submission")*/
    Single<Response<Void>> submitFormNegotiate(
            @Header("Authorization") String authorization,
            @Url String baseUrl
    );

    @Multipart
    @POST/*("submission")*/
    Single<Response<OpenRosaResponseEntity>> submitForm(
            @Header("Authorization") String authorization,
            @Url String baseUrl,
            @PartMap Map<String, RequestBody> parts
    );
}
