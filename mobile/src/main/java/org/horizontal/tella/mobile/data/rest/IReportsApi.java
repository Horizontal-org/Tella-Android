package org.horizontal.tella.mobile.data.rest;

import io.reactivex.Completable;
//import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import org.horizontal.tella.mobile.data.entity.FormMediaFileRegisterEntity;
//import rs.readahead.washington.mobile.data.entity.ReportEntity;


public interface IReportsApi {
    /**
     * Creates new Report on server.
     *
     * @param report Report
     * @return response
     */
    /*@POST("reports")
    Observable<CreateReportResponse> createReport(@Body ReportEntity report);*/

    @POST("media/forms/registrations")
    Completable registerFormAttachments(@Body FormMediaFileRegisterEntity entity);
}
