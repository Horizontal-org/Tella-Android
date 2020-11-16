package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import androidx.annotation.Nullable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;


public class MetadataAttacher implements IMetadataAttachPresenterContract.IPresenter {
    private IMetadataAttachPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public MetadataAttacher(IMetadataAttachPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void attachMetadata(final long mediaFileId, @Nullable final Metadata metadata) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<MediaFile>>() {
                    @Override
                    public SingleSource<MediaFile> apply(DataSource dataSource) throws Exception {
                        return dataSource.attachMetadata(mediaFileId, metadata);
                    }
                })
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(MediaFile mediaFile) throws Exception {
                        view.onMetadataAttached(mediaFileId, metadata);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                        view.onMetadataAttachError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
