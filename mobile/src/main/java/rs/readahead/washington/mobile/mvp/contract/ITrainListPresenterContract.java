package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.TrainModule;


public class ITrainListPresenterContract {
    public interface IView {
        void onTrainModulesSuccess(List<TrainModule> modules);
        void onTrainModulesError(IErrorBundle error);
        void onTrainModulesStarted();
        void onTrainModulesEnded();
        void onTrainModuleRemoved(TrainModule module);
        void onTrainModuleRemoveError(Throwable throwable);
        void onTrainModuleDownloadStarted(TrainModule module);
        void onTrainModuleDownloadError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getModules();
        void searchModules(String ident);
        void listModules();
        void downloadTrainModule(TrainModule module);
        void removeTrainModule(TrainModule module);
        TrainModule getLocalTrainModule(long id);
    }
}
