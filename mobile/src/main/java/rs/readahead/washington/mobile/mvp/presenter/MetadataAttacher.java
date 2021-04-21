package rs.readahead.washington.mobile.mvp.presenter;

import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;


public class MetadataAttacher implements IMetadataAttachPresenterContract.IPresenter {
    private IMetadataAttachPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;


    public MetadataAttacher(IMetadataAttachPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void attachMetadata(final String mediaFileId, @Nullable final Metadata metadata) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<VaultFile>>() {
                    @Override
                    public SingleSource<VaultFile> apply(DataSource dataSource) throws Exception {
                        return dataSource.attachMetadata(mediaFileId, metadata);
                    }
                })
                .subscribe(mediaFile -> view.onMetadataAttached(mediaFileId, metadata), new Consumer<Throwable>() {
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
        view = null;
    }
}
