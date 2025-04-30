package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILES_FILTER;
import static org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_PICKER_SINGLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
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


@SuppressLint("ViewConstructor")
public class UwaziImageWidget extends UwaziFileBinaryWidget {
    AppCompatButton selectButton;
    ImageButton clearButton;
    TextView descriptionTextView;

    private CollectAttachmentPreviewView attachmentPreview;


    public UwaziImageWidget(Context context, UwaziEntryPrompt formEntryPrompt) {
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
        if (data instanceof FormMediaFile) {
            FormMediaFile formMediaFile = (FormMediaFile) data;
            setFilename(formMediaFile.name);
            setFile(formMediaFile);
            setFileId(formMediaFile.id);
            showPreview();
            return getFilename();
        }

        ArrayList<String> files = new Gson().fromJson((String) data, new TypeToken<List<String>>() {
        }.getType());
        if (!files.isEmpty() && !files.get(0).isEmpty()) {
            RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
            VaultFile vaultFile = rxVault
                    .get(files.get(0))
                    .subscribeOn(Schedulers.io())
                    .blockingGet();

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
        selectButton.setText(getContext().getString(R.string.Uwazi_WidgetImage_Select_Text));
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showSelectFilesSheet());

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));
        clearButton.setOnClickListener(v -> clearAnswer());

        attachmentPreview = view.findViewById(R.id.attachedMedia);
        descriptionTextView = view.findViewById(R.id.textview_media_description);
        descriptionTextView.setText(getContext().getString(R.string.Uwazi_WidgetImage_Select_Description_Text));

        if (getFilename() != null) {
            showPreview();
        } else {
            hidePreview();
        }
    }

    private void showAttachmentsFragment() {
        try {
            Activity activity = (Activity) getContext();
            waitingForAData = true;
            List<VaultFile> files = new ArrayList<>();
            RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
            VaultFile vaultFile = getFilename() != null ? rxVault
                    .get(getFileId())
                    .subscribeOn(Schedulers.io())
                    .blockingGet() : null;

            files.add(vaultFile);

            activity.startActivityForResult(new Intent(getContext(), AttachmentsActivitySelector.class)
                            .putExtra(VAULT_FILES_FILTER, FilterType.PHOTO)
                            .putExtra(VAULT_PICKER_SINGLE, true),
                    C.MEDIA_FILE_ID);

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void showCameraActivity() {
        try {
            Activity activity = (Activity) getContext();
            waitingForAData = true;

            activity.startActivityForResult(new Intent(getContext(), CameraActivity.class)
                            .putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT .name())
                            .putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name()),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public void importPhoto() {
        BaseActivity activity = (BaseActivity) getContext();
        activity.maybeChangeTemporaryTimeout(() -> {
            waitingForAData = true;
            MediaFileHandler.startSelectMediaActivity(activity, "image/*", null, C.IMPORT_IMAGE);
            return Unit.INSTANCE;
        });
    }

    private void showSelectFilesSheet() {
        VaultSheetUtils.showVaultSelectFilesSheet(
                ((BaseActivity) getContext()).getSupportFragmentManager(),
                getContext().getString(R.string.Uwazi_WidgetMedia_Take_Photo),
                null,
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                getContext().getString(R.string.Uwazi_Widget_Sheet_Description),
                getContext().getString(R.string.Uwazi_WidgetImage_Select_Description_Text),
                new VaultSheetUtils.IVaultFilesSelector() {

                    @Override
                    public void importFromVault() {
                        showAttachmentsFragment();
                    }

                    @Override
                    public void goToRecorder() {
                    }

                    @Override
                    public void goToCamera() {
                        showCameraActivity();
                    }

                    @Override
                    public void importFromDevice() {
                        importPhoto();
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
