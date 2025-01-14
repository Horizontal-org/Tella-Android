package org.horizontal.tella.mobile.data.rest;


public class ReportsApi extends BaseApi {
    private static final IReportsApi api = getApi(IReportsApi.class);

    public static IReportsApi getApi() {
        return api;
    }
}
