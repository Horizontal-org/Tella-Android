package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.disposables.CompositeDisposable;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadsPresenterContract;

public class TellaFileUploadsPresenter implements ITellaFileUploadsPresenterContract.IPresenter {
    private ITellaFileUploadsPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public TellaFileUploadsPresenter( ITellaFileUploadsPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }

    @Override
    public void getFileUploadInstances() {

    }
}
