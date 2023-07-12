package rs.readahead.washington.mobile.views.custom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.CollectAttachemntPreviewViewBinding;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectAttachmentMediaFilePresenter;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.views.activity.viewer.AudioPlayActivity;
import rs.readahead.washington.mobile.views.activity.viewer.PhotoViewerActivity;
import rs.readahead.washington.mobile.views.activity.viewer.VideoViewerActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;


public class CollectAttachmentPreviewView extends LinearLayout implements ICollectAttachmentMediaFilePresenterContract.IView {
    private final CollectAttachemntPreviewViewBinding binding;

    private VaultFile vaultFile;
    private final CollectAttachmentMediaFilePresenter presenter;

    public CollectAttachmentPreviewView(Context context) {
        this(context, null);
    }

    public CollectAttachmentPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollectAttachmentPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        binding = CollectAttachemntPreviewViewBinding.inflate(LayoutInflater.from(context), this, true);

        presenter = new CollectAttachmentMediaFilePresenter(this);
    }

    @Override
    public void onDetachedFromWindow() {
        presenter.destroy();
        super.onDetachedFromWindow();
    }

    public void showPreview(String id) {
        if (id != null) {
            presenter.getMediaFile(id);
        }
    }

    @Override
    public void onGetMediaFileSuccess(VaultFile vaultFile) {
        this.vaultFile = vaultFile;

        if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            binding.thumbView.setId(QuestionWidget.newUniqueId());
            binding.thumbView.setOnClickListener(v -> showVideoViewerActivity());
            binding.thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            showMediaFileInfo();
            loadThumbnail();

        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            binding.thumbView.setImageResource(R.drawable.ic_baseline_headset_24);
            binding.thumbView.setScaleType(ImageView.ScaleType.CENTER);
            binding.thumbView.setOnClickListener(v -> showAudioPlayActivity());

            showMediaFileInfo();

        } else if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            binding.thumbView.setId(QuestionWidget.newUniqueId());
            binding.thumbView.setOnClickListener(v -> showPhotoViewerActivity());
            binding.thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            loadThumbnail();
            showMediaFileInfo();

            binding.audioInfo.setVisibility(GONE);
            binding.videoInfo.setVisibility(GONE);
        } else if (MediaFile.INSTANCE.isTextFileType(vaultFile.mimeType)) {
            binding.thumbView.setImageResource(R.drawable.ic_baseline_assignment_24);
            binding.thumbView.setScaleType(ImageView.ScaleType.CENTER);

            showMediaFileInfo();
            binding.audioInfo.setVisibility(GONE);
            binding.videoInfo.setVisibility(GONE);
        }
    }


    @Override
    public void onGetMediaFileStart() {

    }

    @Override
    public void onGetMediaFileEnd() {
    }

    @Override
    public void onGetMediaFileError(Throwable error) {
        //thumbGradient.setVisibility(VISIBLE);
        binding.thumbView.setImageResource(R.drawable.ic_error);
        Toast.makeText(getContext(), getResources().getText(R.string.collect_form_toast_fail_load_attachment), Toast.LENGTH_LONG).show();
        binding.audioInfo.setVisibility(GONE);
        binding.videoInfo.setVisibility(GONE);
    }

    private void loadThumbnail() {
        Glide.with(getContext())
                .load(vaultFile.thumb)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.thumbView);
    }

    private void showMediaFileInfo() {
        binding.fileName.setText(vaultFile.name);
        binding.fileSize.setText(FileUtil.getFileSizeString(vaultFile.size));
    }

    private void showVideoViewerActivity() {
        if (vaultFile == null) {
            return;
        }

        try {
            Activity activity = (Activity) getContext();
            activity.startActivity(new Intent(getContext(), VideoViewerActivity.class)
                    .putExtra(VideoViewerActivity.VIEW_VIDEO, vaultFile)
                    .putExtra(VideoViewerActivity.NO_ACTIONS, true));
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void showPhotoViewerActivity() {
        if (vaultFile == null) {
            return;
        }

        try {
            Activity activity = (Activity) getContext();
            activity.startActivity(new Intent(getContext(), PhotoViewerActivity.class)
                    .putExtra(PhotoViewerActivity.VIEW_PHOTO, vaultFile)
                    .putExtra(PhotoViewerActivity.NO_ACTIONS, true));
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void showAudioPlayActivity() {
        if (vaultFile == null) {
            return;
        }

        try {
            Activity activity = (Activity) getContext();
            activity.startActivity(new Intent(getContext(), AudioPlayActivity.class)
                    .putExtra(AudioPlayActivity.PLAY_MEDIA_FILE, vaultFile)
                    .putExtra(AudioPlayActivity.NO_ACTIONS, true));
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}
