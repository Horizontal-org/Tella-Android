package rs.readahead.washington.mobile.bus.event;

import org.javarosa.core.model.FormIndex;

import rs.readahead.washington.mobile.bus.IEvent;


public class MediaFileBinaryWidgetCleared implements IEvent {
    public FormIndex formIndex;
    public String filename;

    public MediaFileBinaryWidgetCleared(FormIndex formIndex, String filename) {
        this.formIndex = formIndex;
        this.filename = filename;
    }
}
