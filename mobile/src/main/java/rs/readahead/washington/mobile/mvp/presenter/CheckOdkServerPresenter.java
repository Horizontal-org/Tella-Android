package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICheckOdkServerContract;
import timber.log.Timber;


public class CheckOdkServerPresenter implements
        ICheckOdkServerContract.IPresenter {
    private final IOpenRosaRepository odkRepository;
    private ICheckOdkServerContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean saveAnyway = false;


    public CheckOdkServerPresenter(ICheckOdkServerContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
    }

    @Override
    public void checkServer(final CollectServer server, boolean connectionRequired) {
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

        OpenRosaService.clearCache();

        disposables.add(odkRepository.formList(server)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showServerCheckLoading())
                .doFinally(() -> view.hideServerCheckLoading())
                .subscribe(listFormResult -> {
                    if (listFormResult.getErrors().size() > 0) {
                        IErrorBundle errorBundle = listFormResult.getErrors().get(0);
                        view.onServerCheckFailure(errorBundle);
                    } else {
                        server.setChecked(true);
                        view.onServerCheckSuccess(server);
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
