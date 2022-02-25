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
import java.util.HashMap;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.CameraActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.custom.PanelToggleButton;
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziMultiFileWidget  extends UwaziQuestionWidget {
    private final HashMap<String, FormMediaFile> formFiles = new HashMap<>();

    Context context;
    Button addFiles;
    ImageButton clearButton;
    ViewGroup filesPanel;
    PanelToggleButton filesToggle;
    TextView numberOfFiles;

    private CollectAttachmentPreviewView attachmentPreview;

    public UwaziMultiFileWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

       this.context = context;

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);

        setHelpTextView(getContext().getString(R.string.Uwazi_MiltiFileWidget_Help));
    }

    @Override
    public Object getAnswer() {
        return null;
    }

    @Override
    public void clearAnswer() {
        formFiles.clear();
        numberOfFiles.setVisibility(GONE);
        filesPanel.removeAllViews();
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

        filesPanel = view.findViewById(R.id.files);
        filesToggle = view.findViewById(R.id.toggle_button);
        filesToggle.setOnStateChangedListener(open -> maybeShowAdvancedPanel());
        numberOfFiles = view.findViewById(R.id.numOfFiles);
        addFiles = view.findViewById(R.id.addText);
        addFiles.setOnClickListener(v -> showAttachmentsFragment());

        /*if (!formFiles.isEmpty()) {
            showPreview();
        } else {
            hidePreview();
        }*/
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        clearAnswer();
        ArrayList<VaultFile> files = new Gson().fromJson((String) data, new TypeToken<List<VaultFile>>() {}.getType());

        if (!files.isEmpty()){
            for (VaultFile file : files) {
                FormMediaFile formMediaFile = FormMediaFile.fromMediaFile(file);
                formFiles.put(file.name, formMediaFile);
            }
            showPreview();
            return getFilenames().toString();
        }

        return null;
    }

    private void showAttachmentsFragment() {
        try {

            Activity activity = (Activity) getContext();
            waitingForAData = true;

           List<VaultFile> vaultFiles = getFilenames() != null ? MyApplication.rxVault
                    .get(getFileIds())
                    .subscribeOn(Schedulers.io())
                    .blockingGet() : null;

            List<VaultFile> files = new ArrayList<>(vaultFiles);

            activity.startActivityForResult(new Intent(getContext(), AttachmentsActivitySelector.class)
                            .putExtra(VAULT_FILE_KEY, new Gson().toJson(files))
                            .putExtra(VAULT_FILES_FILTER, FilterType.ALL)
                            .putExtra(VAULT_PICKER_SINGLE,false),
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
        if (filesToggle.isOpen()){
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
        for (FormMediaFile file : formFiles.values()){
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

    private void showPreview(){
        for (FormMediaFile file : formFiles.values()){
            CollectAttachmentPreviewView previewView = new CollectAttachmentPreviewView(context, null, 0);
            filesPanel.addView(previewView);
            previewView.showPreview(file.id);
        }
        numberOfFiles.setText(context.getResources().getQuantityString(R.plurals.Uwazi_MiltiFileWidget_FilesAttached, formFiles.size(), formFiles.size()));
        numberOfFiles.setVisibility(VISIBLE);
        filesToggle.setOpen();
    }
}
