package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;


public class CacheWordDataSource implements ICacheWordSubscriber {
    private AsyncSubject<DataSource> asyncSubject;
    private CacheWordHandler cacheWordHandler;
    private Context context;


    public CacheWordDataSource(Context context) {
        this.context = context.getApplicationContext();
        asyncSubject = AsyncSubject.create();
        cacheWordHandler = new CacheWordHandler(this.context, this);
        cacheWordHandler.connectToService();
    }

    public Observable<DataSource> getDataSource() {
        return asyncSubject;
    }

    public void dispose() {
        if (cacheWordHandler != null) {
            cacheWordHandler.disconnectFromService();
            cacheWordHandler = null;
        }
        context = null;
    }

    @Override
    public void onCacheWordUninitialized() {
    }

    @Override
    public void onCacheWordLocked() {
    }

    @Override
    public void onCacheWordOpened() {
        asyncSubject.onNext(DataSource.getInstance(this.context, cacheWordHandler.getEncryptionKey()));
        asyncSubject.onComplete();
        dispose();
    }
}
