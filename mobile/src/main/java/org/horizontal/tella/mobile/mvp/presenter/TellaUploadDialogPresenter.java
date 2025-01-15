package org.horizontal.tella.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.data.database.DataSource;
import org.horizontal.tella.mobile.data.database.KeyDataSource;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.mvp.contract.ITellaUploadDialogPresenterContract;


public class TellaUploadDialogPresenter implements ITellaUploadDialogPresenterContract.IPresenter {
    private KeyDataSource keyDataSource;
    private ITellaUploadDialogPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    public TellaUploadDialogPresenter(ITellaUploadDialogPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    public void loadServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<TellaReportServer>>>)
                        DataSource::listTellaUploadServers)
                .subscribe(list -> view.onServersLoaded(list),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onServersLoadError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
