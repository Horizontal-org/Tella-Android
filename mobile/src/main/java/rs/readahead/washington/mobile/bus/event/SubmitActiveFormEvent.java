package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class SubmitActiveFormEvent implements IEvent {
    private final String name;


    public SubmitActiveFormEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
