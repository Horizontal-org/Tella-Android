package rs.readahead.washington.mobile.bus;

import io.reactivex.observers.DisposableObserver;


public class EventObserver<T> extends DisposableObserver<T> {
    @Override
    public void onNext(T t) {
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void onError(Throwable exception) {
    }
}
