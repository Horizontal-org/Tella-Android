package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.util.FileUtil;

public class CollectAttachmentMediaFilePresenter implements ICollectAttachmentMediaFilePresenterContract.IPresenter {
    private ICollectAttachmentMediaFilePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;

    public CollectAttachmentMediaFilePresenter(ICollectAttachmentMediaFilePresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void getMediaFile(String fileName) {
        String uid = FileUtil.getBaseName(fileName);

        disposables.add(
                cacheWordDataSource.getDataSource()
                        .flatMapSingle((Function<DataSource, SingleSource<MediaFile>>) dataSource -> dataSource.getMediaFile(uid))
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
        cacheWordDataSource.dispose();
        view = null;
    }
}
