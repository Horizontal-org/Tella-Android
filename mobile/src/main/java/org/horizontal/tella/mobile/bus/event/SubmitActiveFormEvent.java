package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;


public class SubmitActiveFormEvent implements IEvent {
    private final String name;


    public SubmitActiveFormEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
