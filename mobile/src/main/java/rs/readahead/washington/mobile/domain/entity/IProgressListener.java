package rs.readahead.washington.mobile.domain.entity;


public interface IProgressListener {
    void onProgressUpdate(long current, long total);
}
