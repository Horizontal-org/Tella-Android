package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.MyApplication;

public class KeyDataSource {
    private final AsyncSubject<DataSource> asyncSubject;
    private final AsyncSubject<UwaziDataSource> asyncUwaziSubject;
    private final AsyncSubject<ResourceDataSource> asyncResourceSubject;
    private final AsyncSubject<GoogleDriveDataSource> asyncGoogleDriveSubject;
    private final AsyncSubject<DropBoxDataSource> asyncDropBoxSubject;
    private final Context context;

    public KeyDataSource(Context context) {
        this.context = context.getApplicationContext();
        asyncSubject = AsyncSubject.create();
        asyncUwaziSubject = AsyncSubject.create();
        asyncResourceSubject = AsyncSubject.create();
        asyncGoogleDriveSubject = AsyncSubject.create();
        asyncDropBoxSubject = AsyncSubject.create();
    }

    public void initKeyDataSource() {
        try {
            asyncSubject.onNext(DataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncSubject.onComplete();

            asyncUwaziSubject.onNext(UwaziDataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncUwaziSubject.onComplete();

            asyncResourceSubject.onNext(ResourceDataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncResourceSubject.onComplete();

            asyncGoogleDriveSubject.onNext(GoogleDriveDataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncGoogleDriveSubject.onComplete();

            asyncDropBoxSubject.onNext(DropBoxDataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncDropBoxSubject.onComplete();
        } catch (LifecycleMainKey.MainKeyUnavailableException e) {
            e.printStackTrace();
        }
    }

    public Observable<DataSource> getDataSource() {
        return asyncSubject;
    }

    public Observable<UwaziDataSource> getUwaziDataSource() {
        return asyncUwaziSubject;
    }

    public Observable<ResourceDataSource> getResourceDataSource() {
        return asyncResourceSubject;
    }

    public Observable<GoogleDriveDataSource> getGoogleDriveDataSource() {
        return asyncGoogleDriveSubject;
    }

    public Observable<DropBoxDataSource> getDropBoxDataSource() {
        return asyncDropBoxSubject;
    }

}

