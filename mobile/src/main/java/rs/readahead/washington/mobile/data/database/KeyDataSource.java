package rs.readahead.washington.mobile.data.database;

import android.content.Context;

import com.hzontal.tella_locking_ui.common.CredentialsCallback;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.MyApplication;

public class KeyDataSource  {
    private final AsyncSubject<DataSource> asyncSubject;
    private final Context context;


    public KeyDataSource(Context context) {
        this.context = context.getApplicationContext();
        asyncSubject = AsyncSubject.create();
    }

    public void initKeyDataSource() {
        try {
            asyncSubject.onNext(DataSource.getInstance(this.context, MyApplication.getMainKeyHolder().get().getKey().getEncoded()));
            asyncSubject.onComplete();
        } catch (LifecycleMainKey.MainKeyUnavailableException e) {
            e.printStackTrace();
        }

    }

    public Observable<DataSource> getDataSource() {
        return asyncSubject;
    }

}
