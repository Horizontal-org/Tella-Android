package rs.readahead.washington.mobile.domain.repository;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import io.reactivex.Completable;
import rs.readahead.washington.mobile.domain.entity.RawFile;

public interface ITellaUploadsRepository {
    Completable scheduleUploadMediaFiles(List<VaultFile> vaultFiles);
    Completable scheduleUploadMediaFilesWithPriority(List<VaultFile> mediafiles, long uploadServerId, boolean metadata);
    Completable logUploadedFile(RawFile file);
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