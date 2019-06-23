package rs.readahead.washington.mobile.domain.entity.collect;


public class NegotiatedCollectServer extends CollectServer {
    private boolean openRosa = false;
    private String openRosaVersion;
    private int openRosaAcceptContentLength;
    private boolean urlNegotiated;


    public boolean isOpenRosa() {
        return openRosa;
    }

    public void setOpenRosa(boolean openRosa) {
        this.openRosa = openRosa;
    }

    public String getOpenRosaVersion() {
        return openRosaVersion;
    }

    public void setOpenRosaVersion(String openRosaVersion) {
        this.openRosaVersion = openRosaVersion;
    }

    public int getOpenRosaAcceptContentLength() {
        return openRosaAcceptContentLength;
    }

    public void setOpenRosaAcceptContentLength(int openRosaAcceptContentLength) {
        this.openRosaAcceptContentLength = openRosaAcceptContentLength;
    }

    public boolean isUrlNegotiated() {
        return urlNegotiated;
    }

    public void setUrlNegotiated(boolean urlNegotiated) {
        this.urlNegotiated = urlNegotiated;
    }
}