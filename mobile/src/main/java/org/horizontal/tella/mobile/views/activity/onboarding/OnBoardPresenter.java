package org.horizontal.tella.mobile.views.activity.onboarding;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.data.database.DataSource;
import org.horizontal.tella.mobile.data.database.KeyDataSource;
import org.horizontal.tella.mobile.data.database.UwaziDataSource;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;

public class OnBoardPresenter implements IOnBoardPresenterContract.IPresenter {
    private final KeyDataSource keyDataSource;
    private IOnBoardPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public OnBoardPresenter(IOnBoardPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    @Override
    public void create(final TellaReportServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<TellaReportServer>>)
                        dataSource -> dataSource.createTellaUploadServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> view.onCreatedTUServer(server1),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCreateTUServerError(throwable);
                        })
        );
    }

    @Override
    public void create(final CollectServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>)
                        dataSource -> dataSource.createCollectServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> view.onCreatedServer(server1),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCreateCollectServerError(throwable);
                        })
        );
    }

    @Override
    public void create(UWaziUploadServer server) {
        disposables.add(keyDataSource.getUwaziDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<UwaziDataSource, SingleSource<UWaziUploadServer>>)
                        dataSource -> dataSource.createUWAZIServer(server))
                .subscribe(server1 -> view.onCreatedUwaziServer(server1),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCreateCollectServerError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
