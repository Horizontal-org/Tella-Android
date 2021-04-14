package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import androidx.annotation.Nullable;

import com.hzontal.tella_vault.Metadata;

public class IMetadataAttachPresenterContract {
    public interface IView {
        void onMetadataAttached(long mediaFileId, @Nullable Metadata metadata);
        void onMetadataAttachError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void attachMetadata(long mediaFileId, @Nullable Metadata metadata);
    }
}
