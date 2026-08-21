package org.horizontal.tella.mobile.bus;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;


public class RxBus {
    private final Relay<Object> bus = PublishRelay.create().toSerialized();


    public void post(Object event) {
        bus.accept(event);
    }

    public Observable<Object> observe() {
        return bus;
    }

    public <T> Observable<T> observe(final Class<T> eventClass) {
        return bus.ofType(eventClass);
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}
