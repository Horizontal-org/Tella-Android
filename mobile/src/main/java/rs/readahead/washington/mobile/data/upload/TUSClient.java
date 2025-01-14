package rs.readahead.washington.mobile.data.upload;

import android.content.Context;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.data.http.HttpStatus;
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;

// you'll be surprised in what you might find

public class TUSClient {
    private final OkHttpClient okHttpClient;
    private final URI baseUrl;

    private Context context;

    public TUSClient(Context context, String url, String username, String password) {
        this.context = context.getApplicationContext();

        okHttpClient = buildOkHttpClient();
        baseUrl = URI.create(url);
    }

    public Flowable<UploadProgressInfo> upload(VaultFile mediaFile) {
        return getStatus(mediaFile)
                .flatMapPublisher(skipBytes -> appendFile(mediaFile, skipBytes))
                .onErrorReturn(throwable -> mapThrowable(throwable, mediaFile));
    }

    public Single<UploadProgressInfo> check() {
        VaultFile vaultFile = new VaultFile();
        vaultFile.name = "test";

        return getStatus(vaultFile)
                .map(aLong -> new UploadProgressInfo(vaultFile, 0, 0))
                .onErrorReturn(throwable -> mapThrowable(throwable, vaultFile));
    }

    private Single<Long> getStatus(VaultFile vaultFile) {
        final Request request = new Request.Builder()
                .url(getUploadUrl(vaultFile.name))
                .head()
                .build();

        return Single.create(emitter -> {
            try {
                Response response = okHttpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    long skip = Util.parseLong(response.header("content-length"), 0);
                    emitter.onSuccess(skip);
                    return;
                }

                emitter.onError(new UploadError(response));
            } catch (Exception e) {
                emitter.onError(new UploadError(e));
            }
        });
    }

    private Flowable<UploadProgressInfo> appendFile(VaultFile vaultFile, long skipBytes) {
        return Flowable.create(emitter -> {
            try {
                final long size = vaultFile.size;
                final String fileName = vaultFile.name;
                final UploadEmitter uploadEmitter = new UploadEmitter();

                emitter.onNext(new UploadProgressInfo(vaultFile, skipBytes, UploadProgressInfo.Status.STARTED));

                final Request appendRequest = new Request.Builder()
                        .url(getUploadUrl(fileName))
                        .put(new SkippableMediaFileRequestBody(vaultFile, skipBytes,
                                (current, total) -> uploadEmitter.emit(emitter, vaultFile, skipBytes + current, size)))
                        .build();

                Response response = okHttpClient.newCall(appendRequest).execute();

                if (!response.isSuccessful()) {
                    emitter.onError(new UploadError(response));
                    return;
                }

                final Request closeRequest = new Request.Builder()
                        .url(getUploadUrl(fileName))
                        .header("content-length", "0")
                        .post(RequestBody.create(null, new byte[0]))
                        .build();

                response = okHttpClient.newCall(closeRequest).execute();

                if (!response.isSuccessful()) {
                    emitter.onError(new UploadError(response));
                    return;
                }

                emitter.onNext(new UploadProgressInfo(vaultFile, size, UploadProgressInfo.Status.FINISHED));

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(new UploadError(e));
            }
        }, BackpressureStrategy.LATEST);
    }

    @NonNull
    private OkHttpClient buildOkHttpClient() {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
           builder.addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS));
        }



        return builder.build();
    }

    @NonNull
    private String getUploadUrl(String name) {
        return baseUrl.resolve("/").resolve(name).toString();
    }

    private UploadProgressInfo mapThrowable(Throwable throwable, VaultFile vaultFile) {
        Timber.d(throwable);

        UploadProgressInfo.Status status = UploadProgressInfo.Status.ERROR;
        if (throwable instanceof UploadError) {
            status = toStatus(((UploadError) throwable).code);
        } else if (throwable instanceof UnknownHostException) {
            status = UploadProgressInfo.Status.UNKNOWN_HOST;
        }

        return new UploadProgressInfo(vaultFile, 0, status);
    }

    private UploadProgressInfo.Status toStatus(int code) {
        if (HttpStatus.isSuccess(code)) {
            return UploadProgressInfo.Status.OK;
        }

        if (code == HttpStatus.UNAUTHORIZED_401) {
            return UploadProgressInfo.Status.UNAUTHORIZED;
        }

        if (code == HttpStatus.CONFLICT_409) {
            return UploadProgressInfo.Status.CONFLICT;
        }

        if (code == -1 || HttpStatus.isClientError(code) || HttpStatus.isServerError(code)) {
            return UploadProgressInfo.Status.ERROR;
        }

        return UploadProgressInfo.Status.UNKNOWN;
    }

    // maybe there is a better way to emit once per 500ms?
    private static class UploadEmitter {
        private static final long REFRESH_TIME_MS = 500;
        private long time;

        void emit(Emitter<UploadProgressInfo> emitter, VaultFile file, long current, long total) {
            long now = Util.currentTimestamp();

            if (now - time > REFRESH_TIME_MS) {
                time = now;
                emitter.onNext(new UploadProgressInfo(file, current, total));
            }
        }
    }

    private static class UploadError extends Exception {
        int code = -1;

        UploadError(Response response) {
            super(String.format(
                    Locale.ROOT, "Request failed, response code: %d", response.code()));
            code = response.code();
        }

        UploadError(Throwable cause) {
            super(cause);
        }
    }
}
