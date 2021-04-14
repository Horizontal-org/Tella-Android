package rs.readahead.washington.mobile.mvp.presenter;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.mvp.contract.IAudioPlayPresenterContract;


public class AudioPlayPresenter implements
        IAudioPlayPresenterContract.IPresenter {
    private IAudioPlayPresenterContract.IView view;
    private KeyDataSource keyDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();


    public AudioPlayPresenter(IAudioPlayPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void getMediaFile(final long id) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<MediaFile>>>) dataSource -> dataSource.getMediaFiles(new long[]{id}))
                .subscribe(mediaFiles -> {
                    if (mediaFiles.size() != 1) {
                        view.onMediaFileError(new NotFountException());
                    } else {
                        view.onMediaFileSuccess(mediaFiles.get(0));
                    }
                }, throwable -> view.onMediaFileError(throwable))
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
