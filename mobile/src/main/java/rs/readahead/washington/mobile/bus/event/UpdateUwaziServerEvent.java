package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;

public class UpdateUwaziServerEvent implements IEvent {
    private UWaziUploadServer server;

    public UpdateUwaziServerEvent(UWaziUploadServer server) {
        this.server = server;
    }

    public UWaziUploadServer getServer() {
        return server;
    }
}

