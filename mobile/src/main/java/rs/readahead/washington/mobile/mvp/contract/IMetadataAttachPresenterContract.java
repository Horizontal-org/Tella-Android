package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import androidx.annotation.Nullable;

import com.hzontal.tella_vault.Metadata;

public class IMetadataAttachPresenterContract {
    public interface IView {
        void onMetadataAttached(String mediaFileId, @Nullable Metadata metadata);
        void onMetadataAttachError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void attachMetadata(String mediaFileId, @Nullable Metadata metadata);
    }
}
