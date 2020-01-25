package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import org.javarosa.core.model.FormDef;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;


public class ICollectBlankFormListPresenterContract {
    public interface IView {
        void onBlankFormsListResult(ListFormResult listFormResult);
        void onBlankFormsListError(Throwable error);
        void onNoConnectionAvailable();
        void showBlankFormRefreshLoading();
        void hideBlankFormRefreshLoading();
        void onDownloadBlankFormDefSuccess(CollectForm collectForm);
        void onDownloadBlankFormDefStart();
        void onDownloadBlankFormDefEnd();
        void onUpdateBlankFormDefStart();
        void onUpdateBlankFormDefEnd();
        void onBlankFormDefRemoved();
        void onBlankFormDefRemoveError(Throwable error);
        void onUpdateBlankFormDefSuccess(CollectForm collectForm, FormDef formDef);
        void onUserCancel();
        void onFormDefError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void listBlankForms();
        void refreshBlankForms();
        void downloadBlankFormDef(CollectForm collectForm);
        void removeBlankFormDef(CollectForm form);
        void updateBlankFormDef(CollectForm collectForm);
        void userCancel();
    }
}