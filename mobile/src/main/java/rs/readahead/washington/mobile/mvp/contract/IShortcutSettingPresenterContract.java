package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.presentation.entity.Shortcut;
import rs.readahead.washington.mobile.presentation.entity.ShortcutHolder;
import rs.readahead.washington.mobile.presentation.entity.ShortcutPosition;


public class IShortcutSettingPresenterContract {
    public interface IView {
        void onShortcuts(ShortcutHolder holder);
        void onShortcutsError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getShortcuts(ShortcutPosition position);
        void saveShortcut(ShortcutPosition position, Shortcut shortcut);
    }
}
