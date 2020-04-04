package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import org.javarosa.core.model.FormDef;

import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;


public class IHomeScreenPresenterContract {
    public interface IView {
        Context getContext();
        void onPhoneListLoaded(boolean isListEmpty);
        void onPhoneListLoadError(Throwable throwable);
        void getCollectFormSuccess(CollectForm form, FormDef formDef);
        void onCollectFormError(Throwable throwable);
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
        void loadPhoneList();
        void getCollectForm (String formId);
        void countTUServers();
    }
}
