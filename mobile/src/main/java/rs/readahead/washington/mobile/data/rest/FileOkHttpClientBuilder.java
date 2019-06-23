package rs.readahead.washington.mobile.data.rest;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;


public class FileOkHttpClientBuilder {
    private final OkHttpClient.Builder okClientBuilder;


    public FileOkHttpClientBuilder() {
        okClientBuilder = new OkHttpClient.Builder();
    }

    public void setLogLevelHeader() {
        okClientBuilder.addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS));
    }

    public OkHttpClient build() {
        return okClientBuilder.build();
    }
}
