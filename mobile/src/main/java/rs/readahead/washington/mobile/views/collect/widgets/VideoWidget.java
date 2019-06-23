package rs.readahead.washington.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.crashlytics.android.Crashlytics;

import org.javarosa.form.api.FormEntryPrompt;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.CameraActivity;
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;

/**
 * Based on ODK VideoWidget.
 */
@SuppressLint("ViewConstructor")
public class VideoWidget extends MediaFileBinaryWidget {
    ImageButton selectButton;
    ImageButton clearButton;
    ImageButton captureButton;
    ImageButton importButton;
    View separator;

    private CollectAttachmentPreviewView attachmentPreview;


    public VideoWidget(Context context, FormEntryPrompt formEntryPrompt) {
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
        MediaFile mediaFile = (MediaFile) data;
        setFilename(mediaFile.getFileName());
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

        captureButton = addButton(R.drawable.ic_videocam_black_24dp);
        captureButton.setAlpha((float).5);
        captureButton.setId(QuestionWidget.newUniqueId());
        captureButton.setEnabled(!formEntryPrompt.isReadOnly());
        captureButton.setOnClickListener(v -> showCameraActivity());

        selectButton = addButton(R.drawable.ic_menu_gallery);
        selectButton.setAlpha((float).5);
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showAttachmentsActivity());

        importButton = addButton(R.drawable.ic_smartphone_black_24dp);
        importButton.setAlpha((float).5);
        importButton.setId(QuestionWidget.newUniqueId());
        importButton.setEnabled(!formEntryPrompt.isReadOnly());
        importButton.setOnClickListener(v -> importVideo());

        clearButton = addButton(R.drawable.ic_delete_grey_24px);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setOnClickListener(v -> clearAnswer());

        attachmentPreview = view.findViewById(R.id.attachedMedia);
        separator = view.findViewById(R.id.line_separator);

        if (getFilename() != null) {
            showPreview();
        } else {
            hidePreview();
        }
    }

    private void showAttachmentsActivity() {
        try {
            Activity activity = (Activity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            MediaFile mediaFile = getFilename() != null ? MediaFile.fromFilename(getFilename()) : MediaFile.NONE;

            activity.startActivityForResult(new Intent(getContext(), QuestionAttachmentActivity.class)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, mediaFile)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILES_FILTER, IMediaFileRecordRepository.Filter.VIDEO),
                    C.MEDIA_FILE_ID);
        } catch (Exception e) {
            Crashlytics.logException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showCameraActivity() {
        try {
            Activity activity = (Activity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            activity.startActivityForResult(new Intent(getContext(), CameraActivity.class)
                            .putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT.name())
                            .putExtra(CameraActivity.CAMERA_MODE, CameraActivity.Mode.VIDEO.name()),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            Crashlytics.logException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    public void importVideo() {
        Activity activity = (Activity) getContext();
        FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());
        MediaFileHandler.startSelectMediaActivity(activity, "video/mp4", null, C.IMPORT_VIDEO);
    }

    private void showPreview() {
        selectButton.setVisibility(GONE);
        captureButton.setVisibility(GONE);
        importButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        attachmentPreview.showPreview(getFilename());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
        separator.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        selectButton.setVisibility(VISIBLE);
        captureButton.setVisibility(VISIBLE);
        importButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);

        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
        separator.setVisibility(GONE);
    }
}
