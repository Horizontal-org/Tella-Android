package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;

public class CreateUwaziServerEvent implements IEvent {
    private UWaziUploadServer server;

    public CreateUwaziServerEvent(UWaziUploadServer server) {
        this.server = server;
    }

    public UWaziUploadServer getServer() {
        return server;
    }
}
