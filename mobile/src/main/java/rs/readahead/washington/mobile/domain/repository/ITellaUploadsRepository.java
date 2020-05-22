package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;

public interface ITellaUploadsRepository {
    Completable scheduleUploadMediaFiles(List<MediaFile> mediafiles);
    Single<List<MediaFile>> getUploadMediaFiles(UploadStatus status);
    Completable setUploadStatus(long mediaFileId, UploadStatus status, long uploadedSize, boolean retry);
    Completable deleteFileUploadInstances(UploadStatus status);
    Completable deleteFileUploadInstances(long set);
    Completable deleteFileUploadInstance(long id);

    enum UploadStatus {
        UNKNOWN,
        UPLOADED,
        UPLOADING,
        SCHEDULED,
        ERROR
    }
}