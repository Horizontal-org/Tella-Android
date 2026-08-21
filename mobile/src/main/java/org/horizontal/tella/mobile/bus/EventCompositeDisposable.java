package org.horizontal.tella.mobile.bus;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;


public class EventCompositeDisposable {
    private RxBus bus;
    private CompositeDisposable delegate;


    EventCompositeDisposable(RxBus bus) {
        this.bus = bus;
        this.delegate = new CompositeDisposable();
    }

    /**
     * Observes on main thread.
     *
     * @param clazz
     * @param observer
     * @param <T>
     */
    public <T> EventCompositeDisposable wire(Class<T> clazz, DisposableObserver<T> observer) {
        this.delegate.add(bus.observe(clazz)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(observer)
        );

        return this;
    }

    public boolean add(Disposable disposable) {
        return this.delegate.add(disposable);
    }

    public boolean isDisposed() {
        return this.delegate.isDisposed();
    }

    public void dispose() {
        this.delegate.dispose();
    }
}
