package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;


public class ICollectBlankFormListPresenterContract {
    public interface IView {
        void onBlankFormsListResult(ListFormResult listFormResult);
        void onBlankFormsListError(Throwable error);
        void onNoConnectionAvailable();
        void showBlankFormRefreshLoading();
        void hideBlankFormRefreshLoading();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void listBlankForms();
        void refreshBlankForms();

    }
}
