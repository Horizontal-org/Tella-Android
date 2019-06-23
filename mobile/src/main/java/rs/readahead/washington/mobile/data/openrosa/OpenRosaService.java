package rs.readahead.washington.mobile.data.openrosa;

import android.support.annotation.NonNull;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.net.CookieManager;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.data.http.QuotePreservingCookieJar;


public class OpenRosaService {
    private static OpenRosaService instance;
    private Retrofit retrofit;
    private final static Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
    private volatile static CookieJar cookieJar = new QuotePreservingCookieJar(new CookieManager());


    // todo: keep it like this for now, lets see what we need..
    public static synchronized OpenRosaService getInstance() {
        if (instance == null) {
            instance = new OpenRosaService.Builder().build();
        }

        return instance;
    }

    // todo: keep it like this for now, lets see what we need..
    public static synchronized OpenRosaService newInstance(String username, String password) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        Credentials credentials = new Credentials(username, password);
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
        final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);

        DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()
                .with("digest", digestAuthenticator)
                .with("basic", basicAuthenticator)
                .build();

        builder.cookieJar(cookieJar)
                .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache));

        return new OpenRosaService.Builder(builder).build();
    }

    public static synchronized void clearCache() {
        cookieJar = new QuotePreservingCookieJar(new CookieManager());
        authCache.clear();
    }

    private OpenRosaService(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public IOpenRosaApi getServices() {
        return retrofit.create(IOpenRosaApi.class);
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
                    .addInterceptor(new OpenRosaRequestInterceptor());

            if (BuildConfig.DEBUG) {
                okClientBuilder.addNetworkInterceptor(
                        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)); // or BODY
            }
        }

        public OpenRosaService build() {
            // set client to baseRetrofit builder
            retrofitBuilder.client(okClientBuilder.build());

            // build them
            Retrofit retrofit = retrofitBuilder
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(new Persister(new AnnotationStrategy())))
                    .build();

            return new OpenRosaService(retrofit);
        }
    }

    private static class OpenRosaRequestInterceptor implements Interceptor {
        private final String TZ = "GMT";
        private SimpleDateFormat df;


        OpenRosaRequestInterceptor() {
            df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss ", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone(TZ));
        }

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request originalRequest = chain.request();

            Request newRequest = originalRequest.newBuilder()
                    .header("X-OpenRosa-Version", "1.0")
                    .header("Date", df.format(new Date()) + TZ + "+00:00") // OdkCollect does it like this, not "standard"
                    .build();

            return chain.proceed(newRequest);
        }
    }
}
