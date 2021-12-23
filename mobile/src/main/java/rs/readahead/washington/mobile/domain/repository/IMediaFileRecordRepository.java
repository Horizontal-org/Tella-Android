package rs.readahead.washington.mobile.domain.repository;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import rs.readahead.washington.mobile.data.database.DataSource;


public interface IMediaFileRecordRepository {
    interface IMediaFileDeleter {
        boolean delete(VaultFile vaultFile);
    }

    Single<List<VaultFile>> listMediaFiles(Filter filter, Sort sort);
    Maybe<VaultFile> getMediaFileThumbnail(long id);
    Maybe<VaultFile> getMediaFileThumbnail(String uid);
    Single<VaultFile> updateMediaFileThumbnail(VaultFile vaultFile);
    Single<List<VaultFile>> getMediaFiles(long[] ids);
    Single<VaultFile> getMediaFile(long id);
    Single<VaultFile> getMediaFile(final String uid);
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
