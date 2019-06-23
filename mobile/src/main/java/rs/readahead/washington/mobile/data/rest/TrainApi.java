package rs.readahead.washington.mobile.data.rest;


public class TrainApi extends BaseApi {
    private static final ITrainApi api = getApi(ITrainApi.class);

    public static ITrainApi getApi() {
        return api;
    }
}
