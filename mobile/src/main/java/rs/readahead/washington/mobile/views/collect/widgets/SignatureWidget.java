package rs.readahead.washington.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectAttachmentMediaFilePresenter;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity;
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity;
import rs.readahead.washington.mobile.views.activity.SignatureActivity;


/**
 * Based on ODK SignatureWidget.
 */
@SuppressLint("ViewConstructor")
public class SignatureWidget extends MediaFileBinaryWidget implements ICollectAttachmentMediaFilePresenterContract.IView {
    Button signatureButton;
    ImageButton clearButton;
    LinearLayout attachmentPreview;
    ImageView thumbView;
    TextView fileSize;

    private MediaFile mediaFile;
    private CollectAttachmentMediaFilePresenter presenter;
    private RequestManager.ImageModelRequest<MediaFileLoaderModel> glide;

    public SignatureWidget(Context context, FormEntryPrompt formEntryPrompt) {

        super(context, formEntryPrompt);

        setFilename(formEntryPrompt.getAnswerText());

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addSignatureWidgetViews(linearLayout);
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

    private void addSignatureWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.collect_widget_signature, linearLayout, true);
        attachmentPreview = view.findViewById(R.id.attachedMedia);
        thumbView = view.findViewById(R.id.thumbView);
        fileSize = view.findViewById(R.id.fileSize);

        CacheWordDataSource cacheWordDataSource = new CacheWordDataSource(getContext());
        MediaFileHandler mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
        MediaFileUrlLoader glideLoader = new MediaFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);

        glide = Glide.with(getContext()).using(glideLoader);
        presenter = new CollectAttachmentMediaFilePresenter(this);

        signatureButton = view.findViewById(R.id.selectButton);
        signatureButton.setId(QuestionWidget.newUniqueId());
        signatureButton.setEnabled(!formEntryPrompt.isReadOnly());
        signatureButton.setOnClickListener(v -> showSignatureActivity());

        clearButton = addButton(R.drawable.ic_delete_grey_24px);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setOnClickListener(v -> clearAnswer());

        if (getFilename() != null) {
            showPreview();
        } else {
            hidePreview();
        }
    }

    private void showSignatureActivity() {
        try {
            Activity activity = (Activity) getContext();
            FormController.getActive().setIndexWaitingForData(formEntryPrompt.getIndex());

            MediaFile mediaFile = getFilename() != null ? MediaFile.fromFilename(getFilename()) : MediaFile.NONE;

            activity.startActivityForResult(new Intent(getContext(), SignatureActivity.class)
                            .putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, mediaFile),
                    C.MEDIA_FILE_ID
            );
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            FormController.getActive().setIndexWaitingForData(null);
        }
    }

    private void showPreview() {
        signatureButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);

        showPreview(getFilename());
        attachmentPreview.setEnabled(true);
        attachmentPreview.setVisibility(VISIBLE);
    }

    private void hidePreview() {
        signatureButton.setVisibility(VISIBLE);
        clearButton.setVisibility(GONE);

        attachmentPreview.setEnabled(false);
        attachmentPreview.setVisibility(GONE);
    }

    private void showPreview(String filename){
        presenter.getMediaFile(filename);
    }

    @Override
    public void onDetachedFromWindow() {
        presenter.destroy();
        super.onDetachedFromWindow();
    }

    @Override
    public void onGetMediaFileSuccess(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
        thumbView.setId(QuestionWidget.newUniqueId());
        thumbView.setOnClickListener(v -> showPhotoViewerActivity());

        loadThumbnail();
        showMediaFileInfo();
    }

    @Override
    public void onGetMediaFileStart() {
    }

    @Override
    public void onGetMediaFileEnd() {
    }

    @Override
    public void onGetMediaFileError(Throwable error) {
    }

    private void loadThumbnail() {
        glide.load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(thumbView);
    }

    private void showPhotoViewerActivity() {
        if (mediaFile == null) {
            return;
        }

        try {
            Activity activity = (Activity) getContext();
            activity.startActivity(new Intent(getContext(), PhotoViewerActivity.class)
                    .putExtra(PhotoViewerActivity.VIEW_PHOTO, mediaFile)
                    .putExtra(PhotoViewerActivity.NO_ACTIONS, true));
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void showMediaFileInfo() {
        fileSize.setText(String.format(getResources().getString(R.string.collect_form_meta_file_size), FileUtil.getFileSizeString(mediaFile.getSize())));
    }
}
