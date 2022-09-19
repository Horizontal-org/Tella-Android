package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.mvp.contract.IProtectionSettingsPresenterContract;

public class ProtectionSettingsPresenter implements IProtectionSettingsPresenterContract.IPresenter {
    private IProtectionSettingsPresenterContract.IView view;
    private KeyDataSource keyDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();

    public ProtectionSettingsPresenter(IProtectionSettingsPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void countCollectServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<Long>>) DataSource::countCollectServers)
                .subscribe(
                        num -> view.onCountCollectServersEnded(num),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCountCollectServersFailed(throwable);
                        }
                )
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
