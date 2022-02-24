package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectAttachmentPreviewView;
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziMediaWidget extends UwaziFileBinaryWidget {
    ImageButton selectButton;
    ImageButton clearButton;
    View separator;

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
        VaultFile vaultFile = (VaultFile) data;
        FormMediaFile file = FormMediaFile.fromMediaFile(vaultFile);
        setFilename(vaultFile.name);
        setFile(file);
        //setFileId(vaultFile.id);
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

        selectButton = addButton(R.drawable.ic_add_circle_white);
        selectButton.setAlpha(0.5f);
        selectButton.setId(QuestionWidget.newUniqueId());
        selectButton.setEnabled(!formEntryPrompt.isReadOnly());
        selectButton.setOnClickListener(v -> showAttachmentsFragment());

        clearButton = addButton(R.drawable.ic_cancel_rounded);
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

    private void showAttachmentsFragment() {
        try {

          Activity activity  = (Activity) getContext();
            waitingForAData = true;

            VaultFile vaultFile = getFilename() != null ? MyApplication.rxVault
                    .get(getFilename())
                    .subscribeOn(Schedulers.io())
                    .blockingGet() : null;

            activity.startActivityForResult(new Intent(getContext(), AttachmentsActivitySelector.class)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, vaultFile)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILES_FILTER, IMediaFileRecordRepository.Filter.ALL.ordinal()),
                    C.MEDIA_FILE_ID);

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showPreview() {
        selectButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        attachmentPreview.showPreview(getFileId());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
        separator.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        selectButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);

        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
        separator.setVisibility(GONE);
    }
}
