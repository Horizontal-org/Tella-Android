package rs.readahead.washington.mobile.data.repository;

import retrofit2.HttpException;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import timber.log.Timber;


public class XErrorBundle extends Throwable implements IErrorBundle {
    private int code = 0;
    private String message = null;
    private long serverId;
    private String serverName;


    XErrorBundle(final Throwable throwable) {
        super(throwable);

        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;

            try {
                retrofit2.Response response = httpException.response();

                if (response != null) { // for now just this, maybe parse xml later..
                    code = response.code();
                    message = response.message();
                }
            } catch (Throwable e) {
                try {
                    Timber.e(e, "Getting ErrorResponse from Throwable");
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public Throwable getException() {
        return getCause();
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }
}
