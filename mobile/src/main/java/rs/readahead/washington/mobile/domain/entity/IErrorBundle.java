package rs.readahead.washington.mobile.domain.entity;


public interface IErrorBundle {
    Throwable getException();
    int getCode();
    String getMessage();
    long getServerId(); // origin of some openrosa sever errors
    String getServerName(); // origin of some openrosa sever errors
}
