package rs.readahead.washington.mobile.data.rest;


public class FeedbackApi extends BaseApi {
    private static final IFeedbackApi api = getApi(IFeedbackApi.class);

    public static IFeedbackApi getApi() {
        return api;
    }
}
