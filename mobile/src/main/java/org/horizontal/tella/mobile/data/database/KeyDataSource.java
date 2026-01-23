package org.horizontal.tella.mobile.data.database;

import static org.horizontal.tella.mobile.BuildConfig.ENABLE_DROPBOX;
import static org.horizontal.tella.mobile.BuildConfig.ENABLE_GOOGLE_DRIVE;

import android.content.Context;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;
import org.horizontal.tella.mobile.MyApplication;

public class KeyDataSource {
    private final AsyncSubject<DataSource> asyncSubject;
    private final AsyncSubject<UwaziDataSource> asyncUwaziSubject;
    private final AsyncSubject<ResourceDataSource> asyncResourceSubject;
    private final AsyncSubject<GoogleDriveDataSource> asyncGoogleDriveSubject;
    private final AsyncSubject<DropBoxDataSource> asyncDropBoxSubject;
    private final AsyncSubject<NextCloudDataSource> asyncNextCloudSubject;
    private final Context context;

    public KeyDataSource(Context context) {
        this.context = context.getApplicationContext();
        asyncSubject = AsyncSubject.create();
        asyncUwaziSubject = AsyncSubject.create();
        asyncResourceSubject = AsyncSubject.create();
        asyncGoogleDriveSubject = AsyncSubject.create();
        asyncDropBoxSubject = AsyncSubject.create();
        asyncNextCloudSubject = AsyncSubject.create();
    }

    public void initKeyDataSource() {
        try {
            byte[] key = MyApplication.getMainKeyHolder().get().getKey().getEncoded();
            
            // Core data sources (always initialized)
            asyncSubject.onNext(DataSource.getInstance(this.context, key));
            asyncSubject.onComplete();

            asyncUwaziSubject.onNext(UwaziDataSource.getInstance(this.context, key));
            asyncUwaziSubject.onComplete();

            asyncResourceSubject.onNext(ResourceDataSource.getInstance(this.context, key));
            asyncResourceSubject.onComplete();

            asyncNextCloudSubject.onNext(NextCloudDataSource.getInstance(this.context, key));
            asyncNextCloudSubject.onComplete();

            // Conditional cloud data sources (based on build variant)
            if (ENABLE_GOOGLE_DRIVE) {
                asyncGoogleDriveSubject.onNext(GoogleDriveDataSource.getInstance(this.context, key));
                asyncGoogleDriveSubject.onComplete();
            } else {
                // For F-Droid builds, complete without value (or use stub implementation)
                asyncGoogleDriveSubject.onComplete();
            }

            if (ENABLE_DROPBOX) {
                asyncDropBoxSubject.onNext(DropBoxDataSource.getInstance(this.context, key));
                asyncDropBoxSubject.onComplete();
            } else {
                // For F-Droid builds, complete without value (or use stub implementation)
                asyncDropBoxSubject.onComplete();
            }
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

    public Observable<NextCloudDataSource> getNextCloudDataSource() {
        return asyncNextCloudSubject;
    }

}

