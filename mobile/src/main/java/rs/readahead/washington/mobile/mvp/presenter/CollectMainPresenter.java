package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import org.javarosa.core.model.FormDef;

import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.mvp.contract.ICollectMainPresenterContract;


public class CollectMainPresenter implements ICollectMainPresenterContract.IPresenter {
    private ICollectMainPresenterContract.IView view;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();


    public CollectMainPresenter(ICollectMainPresenterContract.IView view) {
        new OpenRosaRepository();
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
    }

    @Override
    public void getBlankFormDef(final CollectForm form) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<DataSource, ObservableSource<FormDef>>) dataSource ->
                        dataSource.getBlankFormDef(form).toObservable()
                )
                .subscribe(
                        formDef -> view.onGetBlankFormDefSuccess(form, formDef),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onFormDefError(throwable);
                        }
                )
        );
    }

    @Override
    public void getInstanceFormDef(final long instanceId) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<CollectFormInstance>>) dataSource ->
                        dataSource.getInstance(instanceId)
                ).subscribe(
                        instance -> view.onInstanceFormDefSuccess(maybeCloneInstance(instance)),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onFormDefError(throwable);
                        }
                )
        );
    }

    @Override
    public void toggleFavorite(final CollectForm collectForm) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<CollectForm>>) dataSource ->
                        dataSource.toggleFavorite(collectForm))
                .subscribe(
                        form -> view.onToggleFavoriteSuccess(form),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onToggleFavoriteError(throwable);
                        }
                )
        );
    }

    @Override
    public void deleteFormInstance(final long id) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteInstance(id))
                .subscribe(
                        () -> view.onFormInstanceDeleteSuccess(),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onFormInstanceDeleteError(throwable);
                        }
                )
        );
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
                            Crashlytics.logException(throwable);
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

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private CollectFormInstance maybeCloneInstance(CollectFormInstance instance) {
        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED) {
            instance.setClonedId(instance.getId()); // we are clone of submitted form
            instance.setId(0);
            instance.setStatus(CollectFormInstanceStatus.UNKNOWN);
            instance.setUpdated(0);
            instance.setInstanceName(instance.getFormName());

            for (FormMediaFile mediaFile: instance.getWidgetMediaFiles()) {
                mediaFile.status = FormMediaFileStatus.UNKNOWN;
            }
        }

        return instance;
    }
}
