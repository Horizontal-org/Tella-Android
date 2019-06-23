package rs.readahead.washington.mobile.data.rest;


public class ErrorResponse extends Response {
    private Error error;


    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
