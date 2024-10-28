package rs.readahead.washington.mobile.data.rest;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class BaseApi {
    private static Retrofit baseRetrofit;


    // going for single static baseRetrofit and apis for now..
    public static Retrofit getBaseRetrofit() {
        synchronized (BaseApi.class) {
            if (baseRetrofit == null) {
                throw new IllegalStateException();
            }

            return baseRetrofit;
        }
    }

    static <A> A getApi(Class<A> clazz) {
        return getBaseRetrofit().create(clazz);
    }


    public static class Builder {
        private final Retrofit.Builder retrofitBuilder;
        private final OkHttpClient.Builder okClientBuilder;


        public Builder() {
            retrofitBuilder = new Retrofit.Builder();
            okClientBuilder = new OkHttpClient.Builder();
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder setLogLevelFull() {
            okClientBuilder.addNetworkInterceptor(
                 new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            return this;
        }

        public void build(String restBase) {
            // set client to baseRetrofit builder
            retrofitBuilder.client(okClientBuilder.build());

            // build them
            synchronized (BaseApi.class) {
                baseRetrofit = retrofitBuilder
                        .baseUrl(restBase)
                         .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
        }
    }
}
