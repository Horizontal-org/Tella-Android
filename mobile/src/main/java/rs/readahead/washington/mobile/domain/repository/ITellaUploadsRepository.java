package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;

public interface ITellaUploadsRepository {
    Completable scheduleUploadMediaFiles(List<MediaFile> mediafiles);
    Single<List<MediaFile>> getUploadMediaFiles(UploadStatus status);
    Completable setUploadStatus(long mediaFileId, UploadStatus status);
    Completable setUploadReschedule(long mediaFileId);
    Completable setUploadedAmount(long mediaFileId, long uploadedAmount);

    enum UploadStatus {
        UPLOADED,
        UPLOADING,
        SCHEDULED,
        ERROR
    }
}