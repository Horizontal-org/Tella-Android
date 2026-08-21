package org.horizontal.tella.mobile.bus.event;

import java.util.Locale;

import org.horizontal.tella.mobile.bus.IEvent;


public class LocaleChangedEvent implements IEvent {
    private Locale locale;

    public LocaleChangedEvent(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
