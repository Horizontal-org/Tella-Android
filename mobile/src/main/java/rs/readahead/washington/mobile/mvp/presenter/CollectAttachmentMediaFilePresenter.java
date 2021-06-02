package rs.readahead.washington.mobile.mvp.presenter;

import com.hzontal.tella_vault.VaultFile;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.util.FileUtil;

public class CollectAttachmentMediaFilePresenter implements ICollectAttachmentMediaFilePresenterContract.IPresenter {
    private ICollectAttachmentMediaFilePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;

    public CollectAttachmentMediaFilePresenter(ICollectAttachmentMediaFilePresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void getMediaFile(String fileName) {
        String uid = FileUtil.getBaseName(fileName);

        disposables.add(
                keyDataSource.getDataSource()
                        .flatMapSingle((Function<DataSource, SingleSource<VaultFile>>) dataSource -> dataSource.getMediaFile(uid))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> view.onGetMediaFileStart())
                        .doFinally(() -> view.onGetMediaFileEnd())
                        .subscribe(mediaFile -> view.onGetMediaFileSuccess(mediaFile), throwable -> view.onGetMediaFileError(throwable))
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
