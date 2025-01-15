package org.horizontal.tella.mobile.domain.exception;

import org.horizontal.tella.mobile.domain.entity.IErrorBundle;


public class OpenRosaSubmissionErrorBundle extends Exception implements IErrorBundle {
    private final int code;


    public OpenRosaSubmissionErrorBundle(int code) {
        super();
        this.code = code;
    }

    @Override
    public Throwable getException() {
        return this;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public long getServerId() {
        return 0;
    }
}
