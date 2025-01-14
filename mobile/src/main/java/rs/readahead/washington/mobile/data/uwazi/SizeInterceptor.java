package rs.readahead.washington.mobile.data.uwazi;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


public class SizeInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();///
        Response response = chain.proceed(request);


        // for request size
        long requestLength = request.body().contentLength();

        // for response size 
        long responseLength = response.body().contentLength();

        return response;
    }


}