package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILES_FILTER;
import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_PICKER_SINGLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import org.horizontal.tella.mobile.util.crash.CrashReporterProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.rx.RxVault;

import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.media.MediaFileHandler;
import org.horizontal.tella.mobile.util.C;
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity;
import org.horizontal.tella.mobile.views.base_ui.BaseActivity;
import org.horizontal.tella.mobile.views.collect.widgets.QuestionWidget;
import org.horizontal.tella.mobile.views.custom.CollectAttachmentPreviewView;
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;
import org.horizontal.tella.mobile.views.interfaces.ICollectEntryInterface;


@SuppressLint("ViewConstructor")
public class UwaziMediaWidget extends UwaziFileBinaryWidget {
    AppCompatButton selectButton;
    ImageButton clearButton;
    TextView descriptionTextView;

    private CollectAttachmentPreviewView attachmentPreview;


    public UwaziMediaWidget(Context context, UwaziEntryPrompt formEntryPrompt) {
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
        VaultFile vaultFile = null;

        if (data instanceof FormMediaFile) {
            FormMediaFile formMediaFile = (FormMediaFile) data;
            setFilename(formMediaFile.name);
            setFile(formMediaFile);
            setFileId(formMediaFile.id);
            showPreview();
            return getFilename();
        }

        if (data instanceof VaultFile) {
            vaultFile = (VaultFile) data;
        } else if (data instanceof String) {
            String payload = (String) data;
            if (payload.startsWith("[")) {
                ArrayList<String> files = new Gson().fromJson(payload, new TypeToken<List<String>>() {
                }.getType());
                if (files != null && !files.isEmpty() && files.get(0) != null && !files.get(0).isEmpty()) {
                    RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
                    vaultFile = rxVault
                            .get(files.get(0))
                            .subscribeOn(Schedulers.io())
                            .blockingGet();
                }
            }
        }

        if (vaultFile != null) {
            FormMediaFile file = FormMediaFile.fromMediaFile(vaultFile);
            setFilename(file.name);
            setFile(file);
            setFileId(file.id);
            showPreview();
            return getFilename();
        }

        return null;
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
        selectButton.setText(getContext().getString(R.string.Uwazi_WidgetMedia_Select_Text));
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showSelectFilesSheet());

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));
        clearButton.setOnClickListener(v -> clearAnswer());

        attachmentPreview = view.findViewById(R.id.attachedMedia);
        descriptionTextView = view.findViewById(R.id.textview_media_description);
        descriptionTextView.setText(getContext().getString(R.string.Uwazi_WidgetMedia_Description_Text));

        if (getFilename() != null) {
            showPreview();
        } else {
            hidePreview();
        }
    }

    private void showAttachmentsFragment() {
        try {
            BaseActivity activity = getBaseActivity();
            if (activity == null) {
                return;
            }
            waitingForAData = true;
            List<VaultFile> files = new ArrayList<>();
            RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();

            VaultFile vaultFile = getFilename() != null ? rxVault
                    .get(getFileId())
                    .subscribeOn(Schedulers.io())
                    .blockingGet() : null;

            files.add(vaultFile);

            activity.startActivityForResult(new Intent(activity, AttachmentsActivitySelector.class)
                        //    .putExtra(VAULT_FILE_KEY, new Gson().toJson(files))
                            .putExtra(VAULT_FILES_FILTER, FilterType.AUDIO_VIDEO)
                            .putExtra(VAULT_PICKER_SINGLE,true),
                    C.MEDIA_FILE_ID);

        } catch (Exception e) {
            CrashReporterProvider.INSTANCE.get().recordException(e);
        }
    }

    public void importMedia() {
        BaseActivity activity = getBaseActivity();
        if (activity == null) {
            return;
        }
        waitingForAData = true;
        activity.maybeChangeTemporaryTimeout(() -> {
            MediaFileHandler.startSelectMediaActivity(activity,"video/*",null, C.IMPORT_VIDEO, false);
            return Unit.INSTANCE;
        });
    }

    private void showVideoActivity() {
        try {
            BaseActivity activity = getBaseActivity();
            if (activity == null) {
                return;
            }
            waitingForAData = true;

            activity.startActivityForResult(new Intent(activity, CameraActivity.class)
                            .putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT.name())
                            .putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.VIDEO.name()),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            CrashReporterProvider.INSTANCE.get().recordException(e);
        }
    }

    private void showAudioRecorderActivity() {
        try {
            BaseActivity activity = getBaseActivity();
            if (activity instanceof ICollectEntryInterface) {
                ((ICollectEntryInterface) activity).openAudioRecorder();
            }
        } catch (Exception e) {
            CrashReporterProvider.INSTANCE.get().recordException(e);
        }
    }

    private void showSelectFilesSheet(){
        BaseActivity activity = getBaseActivity();
        if (activity == null) {
            return;
        }
        VaultSheetUtils.showVaultSelectFilesSheet(
                activity.getSupportFragmentManager(),
                getContext().getString(R.string.Uwazi_WidgetMedia_Take_Video),
                null, //getContext().getString(R.string.Vault_RecordAudio_SheetAction),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                null,
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_Text),
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
                        showVideoActivity();
                    }

                    @Override
                    public void importFromDevice() {
                        importMedia();
                    }

                }

        );
    }

    private void showPreview() {
        selectButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        attachmentPreview.showPreview(getFileId());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        selectButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);
        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
    }
}
