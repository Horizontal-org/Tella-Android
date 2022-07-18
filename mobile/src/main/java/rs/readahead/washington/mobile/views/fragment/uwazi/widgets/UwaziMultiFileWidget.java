package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILES_FILTER;
import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILE_KEY;
import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_PICKER_SINGLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;

import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.CameraActivity;
import rs.readahead.washington.mobile.views.base_ui.BaseActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.custom.PanelToggleButton;
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziMultiFileWidget extends UwaziQuestionWidget {
    private final HashMap<String, FormMediaFile> formFiles = new HashMap<>();

    Context context;
    Button addFiles;
    ImageButton clearButton;
    ViewGroup filesPanel;
    PanelToggleButton filesToggle;
    TextView numberOfFiles;
    Boolean isPdf;
    View infoFilePanel;


    public UwaziMultiFileWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt, boolean isPdf) {
        super(context, formEntryPrompt);

        this.context = context;
        this.isPdf = isPdf;
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);

        setHelpTextView(formEntryPrompt.getHelpText());
        clearAnswer();
    }

    @Override
    public List<UwaziValue> getAnswer() {
        if (formFiles.isEmpty()) {
            return null;
        } else {
            ArrayList<UwaziValue> resultList = new ArrayList<>();
            resultList.add(new UwaziValue(getFileIds()));
            return (resultList);
        }
    }

    @Override
    public void clearAnswer() {
        formFiles.clear();
        infoFilePanel.setVisibility(GONE);
        clearButton.setVisibility(View.GONE);
        filesPanel.removeAllViews();
        addFiles.setText(isPdf ? R.string.Uwazi_MiltiFileWidget_SelectPdfFiles : R.string.Uwazi_MiltiFileWidget_SelectFiles);
    }

    @Override
    public void setFocus(Context context) {

    }

    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multifile, linearLayout, true);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setOnClickListener(v -> clearAnswer());

        infoFilePanel = view.findViewById(R.id.infoFilePanel);
        filesPanel = view.findViewById(R.id.files);
        filesToggle = view.findViewById(R.id.toggle_button);
        filesToggle.setOnStateChangedListener(open -> maybeShowAdvancedPanel());
        numberOfFiles = view.findViewById(R.id.numOfFiles);
        addFiles = view.findViewById(R.id.addText);
        addFiles.setOnClickListener(v -> showSelectFilesSheet()); //showAttachmentsFragment()
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        List<VaultFile> files;
        List<String> result;
        if (data instanceof ArrayList) {
            result = ((List<String>) data);

        } else {
            result = new Gson().fromJson((String) data, new TypeToken<List<String>>() {
            }.getType());
        }
        if (result != null && !result.isEmpty()) {

            String[] ids = Arrays.copyOf(
                    result.toArray(), result.size(),
                    String[].class);
            files = MyApplication.rxVault
                    .get(ids)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
            if (!files.isEmpty()) {
                for (VaultFile file : files) {
                    FormMediaFile formMediaFile = FormMediaFile.fromMediaFile(file);
                    if (!formFiles.containsKey(file.name)) {
                        formFiles.put(file.name, formMediaFile);
                    }
                }
                showPreview();
                return getFilenames().toString();
            }
        }

        return null;
    }

    private void showAttachmentsFragment() {
        try {

            Activity activity = (Activity) getContext();
            waitingForAData = true;

            String[] ids = getFileIds() != null ? getFileIds() : null;

            activity.startActivityForResult(new Intent(activity, AttachmentsActivitySelector.class)
                            .putExtra(VAULT_FILE_KEY, new Gson().toJson(ids))
                            .putExtra(VAULT_FILES_FILTER, isPdf ? FilterType.PDF : FilterType.ALL_WITHOUT_DIRECTORY)
                            .putExtra(VAULT_PICKER_SINGLE, false),
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
                            .putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT.name())
                    ,  //.putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name())
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void maybeShowAdvancedPanel() {
        if (filesToggle.isOpen()) {
            filesPanel.setVisibility(View.VISIBLE);
            filesToggle.setText(R.string.Uwazi_MiltiFileWidget_Hide);
        } else {
            filesPanel.setVisibility(View.GONE);
            filesToggle.setText(R.string.Uwazi_MiltiFileWidget_Show);
        }
    }

    public String[] getFileIds() {
        String[] Ids = new String[formFiles.size()];
        int i = 0;
        for (FormMediaFile file : formFiles.values()) {
            Ids[i++] = file.id;
        }
        return Ids;
    }

    protected List<String> getFilenames() {
        if (formFiles != null) {
            return new ArrayList<>(formFiles.keySet());
        } else {
            return null;
        }
    }

    private void showSelectFilesSheet() {
        if (isPdf){
        VaultSheetUtils.showVaultSelectFilesSheet(
                ((BaseActivity) getContext()).getSupportFragmentManager(),
                null,
                null,
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                getContext().getString(R.string.Uwazi_MiltiFileWidget_ChooseHowToAddPdfFiles),
                getContext().getString(R.string.Uwazi_MiltiFileWidget_SelectPdfFiles),
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
                        importMedia();
                    }
                }
        );
        } else {
            VaultSheetUtils.showVaultSelectFilesSheet(
                    ((BaseActivity) getContext()).getSupportFragmentManager(),
                    getContext().getString(R.string.Uwazi_WidgetMedia_Take_Photo),
                    null, //getContext().getString(R.string.Vault_RecordAudio_SheetAction),
                    getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
                    getContext().getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
                    getContext().getString(R.string.Uwazi_MiltiFileWidget_ChooseHowToAddFiles),
                    getContext().getString(R.string.Uwazi_MiltiFileWidget_SelectFiles),
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
                            importMedia();
                        }
                    }
            );
        }
    }

    public void showAudioRecorderActivity() {

    }

    public void importMedia() {
        BaseActivity activity = (BaseActivity) getContext();
       // activity.maybeChangeTemporaryTimeout();
        waitingForAData = true;
        MediaFileHandler.startSelectMediaActivity(activity, isPdf ? "application/pdf" : "*/*", null, C.IMPORT_FILE);
    }

    public Collection<FormMediaFile> getFiles() {
        return formFiles.values();
    }

    private void showPreview() {
        filesPanel.removeAllViews();
        infoFilePanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        for (FormMediaFile file : formFiles.values()) {
            CollectAttachmentPreviewView previewView = new CollectAttachmentPreviewView(context, null, 0);
            previewView.showPreview(file.id);
            filesPanel.addView(previewView);
        }
        numberOfFiles.setText(context.getResources().getQuantityString(R.plurals.Uwazi_MiltiFileWidget_FilesAttached, formFiles.size(), formFiles.size()));
        addFiles.setText(R.string.Uwazi_MiltiFileWidget_AddMoreFiles);
        filesToggle.setOpen();
    }
}
