package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public interface IMediaFileRecordRepository {
    interface IMediaFileDeleter {
        boolean delete(MediaFile mediaFile);
    }

    Single<MediaFile> registerMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
    Single<MediaFileBundle> registerMediaFileBundle(MediaFileBundle mediaFileBundle);
    Flowable<List<MediaFileBundle>> registerMediaFileBundles(List<MediaFileBundle> mediaFileBundles);
    Single<List<MediaFile>> listMediaFiles(Filter filter, Sort sort);
    Maybe<MediaFileThumbnailData> getMediaFileThumbnail(long id);
    Maybe<MediaFileThumbnailData> getMediaFileThumbnail(String uid);
    Single<MediaFileThumbnailData> updateMediaFileThumbnail(long id, MediaFileThumbnailData data);
    Single<List<MediaFile>> getMediaFiles(long[] ids);
    Single<MediaFile> getMediaFile(long id);
    Single<MediaFile> getMediaFile(final String uid);
    Single<MediaFile> getLastMediaFile();
    Single<MediaFile> deleteMediaFile(MediaFile mediaFile, IMediaFileDeleter deleter);

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
