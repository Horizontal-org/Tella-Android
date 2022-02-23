package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

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

import java.util.HashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.CameraActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.custom.PanelToggleButton;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

public class UwaziMultiFileWidget  extends UwaziQuestionWidget {
    private final HashMap<String, FormMediaFile> files = new HashMap<>();

    Button addFiles;
    ImageButton clearButton;
    ViewGroup filesPanel;
    PanelToggleButton filesToggle;
    TextView numberOfFiles;

    private CollectAttachmentPreviewView attachmentPreview;

    public UwaziMultiFileWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);

       // setFilename(formEntryPrompt.getAnswerText());

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
        files.clear();
        numberOfFiles.setVisibility(GONE);
        filesPanel.removeAllViews();
    }

    @Override
    public void setFocus(Context context) {

    }

    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multifile, linearLayout, true);

        /*captureButton = addButton(R.drawable.ic_menu_camera);
        captureButton.setAlpha(0.5f);
        captureButton.setId(QuestionWidget.newUniqueId());
        captureButton.setEnabled(!formEntryPrompt.isReadOnly());
        captureButton.setOnClickListener(v -> showCameraActivity());

        selectButton = addButton(R.drawable.ic_add_circle_white);
        selectButton.setAlpha(0.5f);
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showAttachmentsFragment());*/

      /*  importButton = addButton(R.drawable.ic_smartphone_white_24dp);
        importButton.setAlpha((float) .5);
        importButton.setId(QuestionWidget.newUniqueId());
        importButton.setEnabled(!formEntryPrompt.isReadOnly());
        importButton.setOnClickListener(v -> importPhoto());*/

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setOnClickListener(v -> clearAnswer());

        //attachmentPreview = view.findViewById(R.id.attachedMedia);
        filesPanel = view.findViewById(R.id.files);
        filesToggle = view.findViewById(R.id.toggle_button);
        filesToggle.setOnStateChangedListener(open -> maybeShowAdvancedPanel());
        numberOfFiles = view.findViewById(R.id.numOfFiles);
        addFiles = view.findViewById(R.id.addText);
        //separator = view.findViewById(R.id.line_separator);

        if (!files.isEmpty()) {
           // showPreview();
        } else {
           // hidePreview();
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

}
