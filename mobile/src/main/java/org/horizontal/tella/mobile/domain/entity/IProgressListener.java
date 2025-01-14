package org.horizontal.tella.mobile.domain.entity;


public interface IProgressListener {
    void onProgressUpdate(long current, long total);
}
