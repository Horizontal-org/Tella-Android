package rs.readahead.washington.mobile.domain.repository;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public interface IMediaFileRecordRepository {
    interface IMediaFileDeleter {
        boolean delete(VaultFile vaultFile);
    }

    Single<VaultFile> registerMediaFile(VaultFile vaultFile, MediaFileThumbnailData thumbnailData);
    Single<MediaFileBundle> registerMediaFileBundle(MediaFileBundle mediaFileBundle);
    Single<List<VaultFile>> listMediaFiles(Filter filter, Sort sort);
    Maybe<MediaFileThumbnailData> getMediaFileThumbnail(long id);
    Maybe<MediaFileThumbnailData> getMediaFileThumbnail(String uid);
    Single<MediaFileThumbnailData> updateMediaFileThumbnail(long id, MediaFileThumbnailData data);
    Single<List<VaultFile>> getMediaFiles(long[] ids);
    Single<VaultFile> getMediaFile(long id);
    Single<VaultFile> getMediaFile(final String uid);
    Single<VaultFile> getLastMediaFile();
    Single<VaultFile> deleteMediaFile(VaultFile vaultFile, IMediaFileDeleter deleter);

    enum Filter {
        ALL,
        PHOTO,
        VIDEO,
        AUDIO,
        WITH_METADATA,
        WITHOUT_METADATA
    }

    enum Sort {
        NEWEST,
        OLDEST
    }
}
