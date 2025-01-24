package org.horizontal.tella.mobile.views.collect.widgets;

import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.RETURN_ODK;
import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILES_FILTER;
import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_PICKER_SINGLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;

import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;
import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;

import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.media.MediaFileHandler;
import org.horizontal.tella.mobile.odk.FormController;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.views.base_ui.BaseActivity;
import org.horizontal.tella.mobile.views.custom.CollectAttachmentPreviewView;
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import org.horizontal.tella.mobile.views.interfaces.ICollectEntryInterface;

/**
 * Based on ODK AudioWidget.
 */
@SuppressLint("ViewConstructor")
public class AudioWidget extends MediaFileBinaryWidget {
    AppCompatButton selectButton;
    ImageButton clearButton;

    private CollectAttachmentPreviewView attachmentPreview;

    public AudioWidget(Context context, FormEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

        setFilename(formEntryPrompt.getAnswerText());

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        super.clearAnswer();
        setFilename(null);
        hidePreview();
    }

    @Override
    public void setFocus(Context context) {
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        VaultFile vaultFile = (VaultFile) data;
        setFilename(vaultFile.id);
        setFileId(vaultFile.id);
        showPreview();
        return getFilename();
    }

    @Override
    public String getBinaryName() {
        return getFilename();
    }

    @SuppressLint("WrongViewCast")
    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.collect_widget_media, linearLayout, true);

        selectButton = view.findViewById(R.id.addText);
        selectButton.setText(getContext().getString(R.string.Collect_MediaWidget_AttachAudioRecording));
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showSelectFilesSheet());

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setOnClickListener(v -> clearAnswer());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));


        attachmentPreview = view.findViewById(R.id.attachedMedia);

        if (getFilename() != null) {
            showPreview();
        } else {
            hidePreview();
        }
    }

    private void showAudioRecorderActivity() {
        try {
            ICollectEntryInterface activity = (ICollectEntryInterface) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            activity.openAudioRecorder();

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    public void importAudio() {
        BaseActivity activity = (BaseActivity) getContext();
        FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());
        activity.maybeChangeTemporaryTimeout(() -> {
            MediaFileHandler.startSelectMediaActivity(activity, "audio/*", null, C.IMPORT_VIDEO);
            return Unit.INSTANCE;
        });

    }

    private void showPreview() {
        selectButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        attachmentPreview.showPreview(getFilename());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        selectButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);

        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
    }

    private void showSelectFilesSheet() {
        VaultSheetUtils.showVaultSelectFilesSheet(
                ((BaseActivity) getContext()).getSupportFragmentManager(),
                null,
                getContext().getString(R.string.Vault_RecordAudio_SheetAction),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                getContext().getString(R.string.Uwazi_Widget_Sheet_Description),
                getContext().getString(R.string.Collect_WidgetAudio_Select_Text),
                new VaultSheetUtils.IVaultFilesSelector() {

                    @Override
                    public void importFromVault() {
                        showAttachmentsFragment();
                    }

                    @Override
                    public void goToRecorder() {
                        showAudioRecorderActivity();
                    }

                    @Override
                    public void goToCamera() {
                    }

                    @Override
                    public void importFromDevice() {
                        importAudio();
                    }

                }

        );
    }

    private void showAttachmentsFragment() {
        try {
            BaseActivity activity = (BaseActivity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());
            List<VaultFile> files = new ArrayList<>();

            VaultFile vaultFile = getFilename() != null ? MyApplication.rxVault
                    .get(getFileId())
                    .subscribeOn(Schedulers.io())
                    .blockingGet() : null;

            files.add(vaultFile);


            activity.startActivityForResult(new Intent(getContext(), AttachmentsActivitySelector.class)
                            .putExtra(RETURN_ODK, true)
                            .putExtra(VAULT_FILES_FILTER, FilterType.AUDIO)
                            .putExtra(VAULT_PICKER_SINGLE, true),
                    C.MEDIA_FILE_ID);

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }
}
