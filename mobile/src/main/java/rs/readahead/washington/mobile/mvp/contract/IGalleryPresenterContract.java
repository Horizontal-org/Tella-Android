package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class IGalleryPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<MediaFile> files);
        void onGetFilesError(Throwable error);
        void onMediaImported(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void onMediaImported(List<MediaFileBundle> mediaFileBundles);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        void onMediaFilesAdded(MediaFile mediaFile);
        void onMediaFilesAdded(List<MediaFileBundle> mediaFileBundles);
        void onMediaFilesAddingError(Throwable error);
        void onMediaFilesDeleted(int num);
        void onMediaFilesDeletionError(Throwable throwable);
        void onMediaExported(int num);
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
        //void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle);
        //void onTmpVideoEncryptionError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(IMediaFileRecordRepository.Filter filter, IMediaFileRecordRepository.Sort sort);
        void importImage(Uri uri);
        void importImages(List<Uri> uris);
        void importVideo(Uri uri);
        void importVideos(List<Uri> uris);
        void addNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData);
        void addNewMediaFiles(List<MediaFileBundle> mediaFileBundles);
        void deleteMediaFiles(List<MediaFile> mediaFiles);
        void exportMediaFiles(List<MediaFile> mediaFiles);
        void countTUServers();
        //void encryptTmpVideo(Uri uri);
    }
}
