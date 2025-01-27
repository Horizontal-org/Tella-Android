package org.horizontal.tella.mobile.bus.event;

import org.javarosa.core.model.FormIndex;

import org.horizontal.tella.mobile.bus.IEvent;


public class MediaFileBinaryWidgetCleared implements IEvent {
    public FormIndex formIndex;
    public String filename;

    public MediaFileBinaryWidgetCleared(FormIndex formIndex, String filename) {
        this.formIndex = formIndex;
        this.filename = filename;
    }
}
