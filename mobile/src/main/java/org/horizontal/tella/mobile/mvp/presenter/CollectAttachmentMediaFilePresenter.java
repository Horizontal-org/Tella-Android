package org.horizontal.tella.mobile.mvp.presenter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import org.horizontal.tella.mobile.util.FileUtil;


public class CollectAttachmentMediaFilePresenter implements ICollectAttachmentMediaFilePresenterContract.IPresenter {
    private ICollectAttachmentMediaFilePresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public CollectAttachmentMediaFilePresenter(ICollectAttachmentMediaFilePresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public void getMediaFile(String vaultFileNameId) {
        String vaultFileId = FileUtil.getBaseName(vaultFileNameId);

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
