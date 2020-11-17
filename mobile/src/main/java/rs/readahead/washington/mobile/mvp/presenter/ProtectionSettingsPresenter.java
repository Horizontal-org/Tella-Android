package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.mvp.contract.IProtectionSettingsPresenterContract;

public class ProtectionSettingsPresenter implements IProtectionSettingsPresenterContract.IPresenter {
    private IProtectionSettingsPresenterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();

    public ProtectionSettingsPresenter(IProtectionSettingsPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
    }

    @Override
    public void countCollectServers() {
        disposables.add(cacheWordDataSource.getDataSource()
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
        cacheWordDataSource.dispose();
        view = null;
    }
}
