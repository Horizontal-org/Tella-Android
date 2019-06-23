package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import android.support.annotation.NonNull;

import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormParserContract {
    interface IView {
        void formBeginning(String title);
        void formEnd(CollectFormInstance instance);
        void formQuestion(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formGroup(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formRepeat(FormEntryPrompt[] prompts, FormEntryCaption[] groups);
        void formPromptNewRepeat(int lastRepeatCount, String groupText);
        void formParseError(Throwable error);
        void formPropertiesChecked(boolean enableDelete);
        Context getContext();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    interface IFormParser extends IBasePresenter {
        void parseForm();
        void stepToNextScreen();
        void stepToPrevScreen();
        boolean isFirstScreen();
        boolean isFormChanged();
        boolean isFormFinal();
        void startFormChangeTracking();
        // List<MediaFile> getFormAttachments();
        void executeRepeat();
        void cancelRepeat();
        void setWidgetMediaFile(@NonNull String name, @NonNull MediaFile mediaFile);
        void removeWidgetMediaFile(String name);
        void stopWaitingBinaryData();
    }
}
