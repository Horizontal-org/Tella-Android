package rs.readahead.washington.mobile.data.openrosa;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.X509TrustManager;

import io.reactivex.Single;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.data.http.QuotePreservingCookieJar;
import rs.readahead.washington.mobile.data.repository.TLSSocketFactory;
import rs.readahead.washington.mobile.data.upload.NetCipherTUSClient;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;

public class OpenRosaNetCipherService  {
    private Retrofit retrofit ;
    private final static Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
    private volatile static CookieJar cookieJar = new QuotePreservingCookieJar(new CookieManager());
    private final NetCipherTUSClient.IOnNetCipherConnect onNetCipherConnect = null;
    private NetCipherStrongBuilder netCipherStrongBuilder;
    private OkHttpClient okHttpClient  ;
    private Context context;

    public OpenRosaNetCipherService(Context context){
        this.context = context;
    }

    public Single<OpenRosaNetCipherService> connect(String username, String password){
        return Single.create(emitter -> {
            try {
                new NetCipherStrongBuilder(context, new NetCipherStrongBuilder.IOnNetCipherConnect() {
                    @Override
                    public void onConnected(OkHttpClient okHttpClient) {
                        setOkHttpClient(okHttpClient);
                        emitter.onSuccess(newInstance(username, password));
                    }

                    @Override
                    public void onException(Exception e) {
                        emitter.onError(e);
                    }
                }).init();

            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
    // todo: keep it like this for now, lets see what we need..
    private OpenRosaNetCipherService newInstance(String username, String password) {
        // enable TLS 1.2 explicitly (allow androids < 5.1 to use it)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                TLSSocketFactory tlsSocketFactory = new TLSSocketFactory();
                X509TrustManager trustManager = tlsSocketFactory.getTrustManager();

                if (trustManager != null) {
                    okHttpClient.newBuilder().sslSocketFactory(tlsSocketFactory, trustManager);

                    List<ConnectionSpec> specs = new ArrayList<>();
                    specs.add(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build());
                    specs.add(ConnectionSpec.COMPATIBLE_TLS);
                    specs.add(ConnectionSpec.CLEARTEXT);

                    okHttpClient.newBuilder().connectionSpecs(specs);
                }
            } catch (Exception e) {
                Timber.d(e);
            }
        }

        Credentials credentials = new Credentials(username, password);
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
        final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);

        DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()
                .with("digest", digestAuthenticator)
                .with("basic", basicAuthenticator)
                .build();

        okHttpClient.newBuilder().cookieJar(cookieJar)
                .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache));

        return new OpenRosaNetCipherService.Builder().build();
    }

    public IOpenRosaApi getServices() {
        return retrofit.create(IOpenRosaApi.class);
    }
    private OpenRosaNetCipherService(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public static class Builder {
        private final Retrofit.Builder retrofitBuilder;
        private OkHttpClient okHttpClient;

        public Builder() {
            this(new OkHttpClient.Builder());
        }

        public Builder(OkHttpClient.Builder builder) {
            retrofitBuilder = new Retrofit.Builder();
            retrofitBuilder.baseUrl("https://www.hzontal.org/"); // dummy baseUrl to keep retrofit happy, all calls have @Url parameter

            okHttpClient.newBuilder()
                    .proxy(Proxy.NO_PROXY)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .addInterceptor(new OpenRosaRequestInterceptor());

            if (BuildConfig.DEBUG) {
                okHttpClient.newBuilder().addNetworkInterceptor(
                        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)); // or BODY
            }
        }

        public OpenRosaNetCipherService build() {
            // set client to baseRetrofit builder
            retrofitBuilder.client(okHttpClient.newBuilder().build());

            // build them
            Retrofit retrofit = retrofitBuilder
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(new Persister(new AnnotationStrategy())))
                    .build();

            return new OpenRosaNetCipherService(retrofit);
        }
    }

    private static class OpenRosaRequestInterceptor implements Interceptor {
        private final String TZ = "GMT";
        private final SimpleDateFormat df;


        OpenRosaRequestInterceptor() {
            df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss ", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone(TZ));
        }

        @NonNull
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
