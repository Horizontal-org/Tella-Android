package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;

public class UpdateUwaziServerEvent implements IEvent {
    private UWaziUploadServer server;

    public UpdateUwaziServerEvent(UWaziUploadServer server) {
        this.server = server;
    }

    public UWaziUploadServer getServer() {
        return server;
    }
}

