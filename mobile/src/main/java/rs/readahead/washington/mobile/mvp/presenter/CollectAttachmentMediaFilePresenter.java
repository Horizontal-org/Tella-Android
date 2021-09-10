package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;

public class CollectAttachmentMediaFilePresenter implements ICollectAttachmentMediaFilePresenterContract.IPresenter {
    private ICollectAttachmentMediaFilePresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public CollectAttachmentMediaFilePresenter(ICollectAttachmentMediaFilePresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public void getMediaFile(String vaultFileId) {
        disposables.add(MyApplication.rxVault.get(vaultFileId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> view.onGetMediaFileStart())
                        .doFinally(() -> view.onGetMediaFileEnd())
                        .subscribe(vaultFile -> view.onGetMediaFileSuccess(vaultFile),
                                throwable -> view.onGetMediaFileError(throwable))
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
