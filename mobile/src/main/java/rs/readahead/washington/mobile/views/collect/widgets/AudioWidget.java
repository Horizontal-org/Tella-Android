package rs.readahead.washington.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.VaultUtils;

import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.AudioRecordActivity2;
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;

/**
 * Based on ODK AudioWidget.
 */
@SuppressLint("ViewConstructor")
public class AudioWidget extends MediaFileBinaryWidget {
    ImageButton selectButton;
    ImageButton clearButton;
    ImageButton captureButton;
    View separator;

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
        setFilename(vaultFile.name);
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

        captureButton = addButton(R.drawable.ic_mic_black_24dp);
        captureButton.setAlpha((float).5);
        captureButton.setId(QuestionWidget.newUniqueId());
        captureButton.setEnabled(!formEntryPrompt.isReadOnly());
        captureButton.setOnClickListener(v -> showAudioRecorderActivity());

        selectButton = addButton(R.drawable.ic_menu_gallery);
        selectButton.setAlpha((float).5);
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showAttachmentsActivity());

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

            VaultFile mediaFile = getFilename() != null ? VaultUtils.INSTANCE.fromFilename(getFilename()) : null;

            activity.startActivityForResult(new Intent(getContext(), QuestionAttachmentActivity.class)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, mediaFile)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILES_FILTER, IMediaFileRecordRepository.Filter.AUDIO),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showAudioRecorderActivity() {
        try {
            Activity activity = (Activity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            activity.startActivityForResult(new Intent(getContext(), AudioRecordActivity2.class)
                            .putExtra(AudioRecordActivity2.RECORDER_MODE, AudioRecordActivity2.Mode.COLLECT.name()),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showPreview() {
        selectButton.setVisibility(GONE);
        captureButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        attachmentPreview.showPreview(getFilename());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
        separator.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        selectButton.setVisibility(VISIBLE);
        captureButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);

        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
        separator.setVisibility(GONE);
    }
}
