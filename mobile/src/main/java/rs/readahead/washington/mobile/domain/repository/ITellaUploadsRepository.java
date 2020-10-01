package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.RawFile;

public interface ITellaUploadsRepository {
    Completable scheduleUploadMediaFiles(List<MediaFile> mediafiles);
    Completable scheduleUploadMediaFilesWithPriority(List<RawFile> mediafiles);
    Completable logUploadedFile(RawFile file);
    Single<List<MediaFile>> getUploadMediaFiles(UploadStatus status);
    Completable setUploadStatus(long mediaFileId, UploadStatus status, long uploadedSize, boolean retry);
    Completable deleteFileUploadInstancesInStatus(UploadStatus status);
    Completable deleteFileUploadInstancesNotInStatus(UploadStatus status);
    Completable deleteFileUploadInstancesBySet(long set);
    Completable deleteFileUploadInstanceById(long id);

    enum UploadStatus {
        UNKNOWN,
        UPLOADED,
        UPLOADING,
        SCHEDULED,
        ERROR
    }
}