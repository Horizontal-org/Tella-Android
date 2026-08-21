package org.horizontal.tella.mobile.data.rest;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;
import org.horizontal.tella.mobile.data.entity.TrainModuleEntity;


public interface ITrainApi {
    @GET("train/modules")
    Single<List<TrainModuleEntity>> listTrainModules();

    @GET("train/modules")
    Single<List<TrainModuleEntity>> searchTrainModules(@Query("ident") String ident);
}
