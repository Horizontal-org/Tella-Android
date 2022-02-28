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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.CameraActivity;
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
        addFiles.setOnClickListener(v -> showAttachmentsFragment());
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        clearAnswer();
        List<VaultFile> files = null;
        List<String> result = null;
        if (data instanceof ArrayList) {
            result = ((List<String>) data);

        } else {
            result  = new Gson().fromJson((String) data, new TypeToken<List<String>>() {
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
            if(!files.isEmpty()){
                for (VaultFile file : files) {
                    FormMediaFile formMediaFile = FormMediaFile.fromMediaFile(file);
                    formFiles.put(file.name, formMediaFile);
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

            activity.startActivityForResult(new Intent(getContext(), AttachmentsActivitySelector.class)
                            .putExtra(VAULT_FILE_KEY, new Gson().toJson(ids))
                            .putExtra(VAULT_FILES_FILTER, isPdf ? FilterType.DOCUMENTS : FilterType.ALL)
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
                            .putExtra(CameraActivity.CAMERA_MODE, CameraActivity.CameraMode.PHOTO.name()),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
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

    public Collection<FormMediaFile> getFiles() {
        return formFiles.values();
    }

    private void showPreview() {
        infoFilePanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        for (FormMediaFile file : formFiles.values()) {
            CollectAttachmentPreviewView previewView = new CollectAttachmentPreviewView(context, null, 0);
            filesPanel.addView(previewView);
            previewView.showPreview(file.id);
        }
        numberOfFiles.setText(context.getResources().getQuantityString(R.plurals.Uwazi_MiltiFileWidget_FilesAttached, formFiles.size(), formFiles.size()));
        addFiles.setText(R.string.Uwazi_MiltiFileWidget_AddMoreFiles);
        filesToggle.setOpen();
    }
}
