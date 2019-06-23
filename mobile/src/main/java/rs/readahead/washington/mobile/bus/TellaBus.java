package rs.readahead.washington.mobile.bus;

import io.reactivex.disposables.CompositeDisposable;


public class TellaBus extends RxBus {
    private final CompositeDisposable disposables;
    private TellaBus() {
        disposables = new CompositeDisposable();
    }


    public static TellaBus create() {
        TellaBus bus = new TellaBus();

        // application lifecycle events and handlers..
        bus.wireApplicationEvents();

        return bus;
    }

    public EventCompositeDisposable createCompositeDisposable() {
        return new EventCompositeDisposable(this);
    }

    private void wireApplicationEvents() {
    }
}
