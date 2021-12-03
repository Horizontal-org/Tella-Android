package rs.readahead.washington.mobile.data.openrosa;

import android.content.Context;
import android.os.Build;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import rs.readahead.washington.mobile.data.repository.TLSSocketFactory;
import timber.log.Timber;

public class NetCipherStrongBuilder implements StrongBuilder.Callback<OkHttpClient> {
    private final Context context;
    private final IOnNetCipherConnect onNetCipherConnect;

    NetCipherStrongBuilder(Context context, IOnNetCipherConnect onNetCipherConnect) {
        this.onNetCipherConnect = onNetCipherConnect;
        this.context = context.getApplicationContext();
    }

    public void init(){
        try {
            StrongOkHttpClientBuilder
                    .forMaxSecurity(context)
                    .withBestProxy()
                    .build(this);
        } catch (Exception e) {
            e.printStackTrace();
            onNetCipherConnect.onException(e);
        }
    }

    @Override
    public void onConnected(OkHttpClient okHttpClient) {
        onNetCipherConnect.onConnected(okHttpClient);
    }

    @Override
    public void onConnectionException(Exception e) {
        Timber.d(e);
        onNetCipherConnect.onException(e);
    }

    @Override
    public void onTimeout() {
        onNetCipherConnect.onException(new Exception("** Timeout **"));
    }

    @Override
    public void onInvalid() {
        onNetCipherConnect.onException(new Exception("** INVALID CONNEXION **"));
    }

    public interface IOnNetCipherConnect {
        void onConnected(OkHttpClient okHttpClient);
        void onException(Exception e);
    }
}

