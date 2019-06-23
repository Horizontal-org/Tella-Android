package rs.readahead.washington.mobile.domain.repository;

import android.support.annotation.NonNull;

import java.util.Collection;

import io.reactivex.Completable;
import rs.readahead.washington.mobile.domain.entity.MediaFile;


public interface IMediaFileRepository {
    Completable registerFormAttachments(@NonNull Collection<MediaFile> mediaFile);
}
