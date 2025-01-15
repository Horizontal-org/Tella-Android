package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;

public class CreateUwaziServerEvent implements IEvent {
    private UWaziUploadServer server;

    public CreateUwaziServerEvent(UWaziUploadServer server) {
        this.server = server;
    }

    public UWaziUploadServer getServer() {
        return server;
    }
}
