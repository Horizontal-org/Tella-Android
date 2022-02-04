package rs.readahead.washington.mobile.data.uwazi;

import android.os.Build;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.ihsanbal.logging.Level;
import com.ihsanbal.logging.LoggingInterceptor;

import java.net.CookieManager;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.TlsVersion;
import okhttp3.internal.platform.Platform;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.data.http.QuotePreservingCookieJar;
import rs.readahead.washington.mobile.data.repository.TLSSocketFactory;
import rs.readahead.washington.mobile.data.rest.IUwaziApi;
import timber.log.Timber;

public class UwaziService {
    private static UwaziService instance;
    private final Retrofit retrofit;
    private final static Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
    private volatile static CookieJar cookieJar = new QuotePreservingCookieJar(new CookieManager());

    // todo: keep it like this for now, lets see what we need..
    public static synchronized UwaziService getInstance() {
        if (instance == null) {
            instance = new UwaziService.Builder().build();
        }
        return instance;
    }

    // todo: keep it like this for now, lets see what we need..
    public static synchronized UwaziService newInstance() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // enable TLS 1.2 explicitly (allow androids < 5.1 to use it)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                TLSSocketFactory tlsSocketFactory = new TLSSocketFactory();
                X509TrustManager trustManager = tlsSocketFactory.getTrustManager();

                if (trustManager != null) {
                    builder.sslSocketFactory(tlsSocketFactory, trustManager);

                    List<ConnectionSpec> specs = new ArrayList<>();
                    specs.add(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build());
                    specs.add(ConnectionSpec.COMPATIBLE_TLS);
                    specs.add(ConnectionSpec.CLEARTEXT);

                    builder.connectionSpecs(specs);
                }
            } catch (Exception e) {
                Timber.d(e);
            }
        }


        builder.cookieJar(cookieJar)
                .addInterceptor(new AuthenticationCacheInterceptor(authCache));


        return new UwaziService.Builder(builder).build();
    }

    public static synchronized void clearCache() {
        cookieJar = new QuotePreservingCookieJar(new CookieManager());
        authCache.clear();
    }

    private UwaziService(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public IUwaziApi getServices() {
        return retrofit.create(IUwaziApi.class);
    }

    public static class Builder {
        private final Retrofit.Builder retrofitBuilder;
        private final OkHttpClient.Builder okClientBuilder;


        public Builder() {
            this(new OkHttpClient.Builder());
        }

        public Builder(OkHttpClient.Builder builder) {
            retrofitBuilder = new Retrofit.Builder();
            retrofitBuilder.baseUrl("https://www.hzontal.org/"); // dummy baseUrl to keep retrofit happy, all calls have @Url parameter

            okClientBuilder = builder
                    .proxy(Proxy.NO_PROXY)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .cookieJar(new UvCookieJar());


            LoggingInterceptor logger = new LoggingInterceptor.Builder()
                    .loggable(true)
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
                    .request("Request")
                    .response("Response")
                    .build();


            if (BuildConfig.DEBUG) {
                okClientBuilder.addNetworkInterceptor(
                        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .addInterceptor(logger);
                // or BODY


            }
        }

        public UwaziService build() {
            // set client to baseRetrofit builder
            retrofitBuilder.client(okClientBuilder.build());


            // build them
            Retrofit retrofit = retrofitBuilder
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())

                    .build();



            return new UwaziService(retrofit);
        }
    }


}
