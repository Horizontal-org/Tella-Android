package rs.readahead.washington.mobile.views.collect.widgets;

import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.RETURN_ODK;
import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILES_FILTER;
import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_PICKER_SINGLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;

import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;
import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.base_ui.BaseActivity;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import rs.readahead.washington.mobile.views.interfaces.ICollectEntryInterface;
import timber.log.Timber;

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
            /*activity.startActivityForResult(new Intent(getContext(), AudioRecordActivity2.class)
                            .putExtra(AudioRecordActivity2.RECORDER_MODE, AudioRecordActivity2.Mode.COLLECT.name()),
                    C.MEDIA_FILE_ID
            );*/
        } catch (Exception e) {
            Timber.e(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    public void importAudio() {
        Activity activity = (Activity) getContext();
        FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());
        MediaFileHandler.startSelectMediaActivity(activity, "audio/*", null, C.IMPORT_VIDEO);
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

    private void showSelectFilesSheet(){
        VaultSheetUtils.showVaultSelectFilesSheet(
                ((BaseActivity) getContext()).getSupportFragmentManager(),
                null,
                getContext().getString(R.string.Vault_RecordAudio_SheetAction),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                getContext().getString(R.string.Uwazi_Widget_Sheet_Description),
                getContext().getString(R.string.Collect_WidgetAudio_Select_Text),
                new  VaultSheetUtils.IVaultFilesSelector() {

                    @Override
                    public void  importFromVault(){
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
            Activity activity = (Activity) getContext();
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
            Timber.e(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }
}
