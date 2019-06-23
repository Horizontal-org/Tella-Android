package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import org.javarosa.core.model.FormDef;

import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;


public class ICollectMainPresenterContract {
    public interface IView {
        void onGetBlankFormDefSuccess(CollectForm collectForm, FormDef formDef);
        void onDownloadBlankFormDefSuccess(CollectForm collectForm, FormDef formDef);
        void onInstanceFormDefSuccess(CollectFormInstance instance); // todo: something is wrong here, but lets do it for now :)
        void onToggleFavoriteSuccess(CollectForm form);
        void onToggleFavoriteError(Throwable error);
        void onFormDefError(Throwable error);
        void onFormInstanceDeleteSuccess();
        void onFormInstanceDeleteError(Throwable throwable);
        Context getContext();
        void onCountCollectServersEnded(long num);
        void onCountCollectServersFailed(Throwable throwable);
        void onBlankFormDefRemoved();
        void onBlankFormDefRemoveError(Throwable error);
        void onUpdateBlankFormDefSuccess(CollectForm collectForm, FormDef formDef);
    }

    public interface IPresenter extends IBasePresenter {
        void getBlankFormDef(CollectForm collectForm);
        void downloadBlankFormDef(CollectForm collectForm);
        void getInstanceFormDef(long instanceId);
        void toggleFavorite(CollectForm collectForm);
        void deleteFormInstance(long id);
        void countCollectServers();
        void removeBlankFormDef(CollectForm form);
        void updateBlankFormDef(CollectForm collectForm);
    }
}
