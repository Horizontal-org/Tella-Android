package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.mvp.contract.ICheckTUSServerContract;
import timber.log.Timber;


public class CheckTUSServerPresenter implements
        ICheckTUSServerContract.IPresenter {
    private ICheckTUSServerContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean saveAnyway = false;


    public CheckTUSServerPresenter(ICheckTUSServerContract.IView view) {
        this.view = view;
    }

    @Override
    public void checkServer(final TellaUploadServer server, boolean connectionRequired) {
        if (! MyApplication.isConnectedToInternet(view.getContext())) {
            if (saveAnyway && !connectionRequired) {
                server.setChecked(false);
                view.onServerCheckSuccess(server);
            } else {
                view.onNoConnectionAvailable();
                setSaveAnyway(true);
            }
            return;
        } else {
            if (saveAnyway) {
                setSaveAnyway(false);
            }
        }

        TUSClient client = new TUSClient(view.getContext().getApplicationContext(),
                server.getUrl(), server.getUsername(), server.getPassword());

        OpenRosaService.clearCache();

        disposables.add(client.check()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showServerCheckLoading())
                .doFinally(() -> view.hideServerCheckLoading())
                .subscribe((uploadProgressInfo) -> {
                    if (uploadProgressInfo.status == UploadProgressInfo.Status.OK) {
                        server.setChecked(true);
                        view.onServerCheckSuccess(server);
                    } else {
                        view.onServerCheckFailure(uploadProgressInfo.status);
                    }
                }, throwable -> {
                    Timber.e(throwable);
                    view.onServerCheckError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    private void setSaveAnyway(boolean enable) {
        saveAnyway = enable;
        view.setSaveAnyway(enable);
    }
}
