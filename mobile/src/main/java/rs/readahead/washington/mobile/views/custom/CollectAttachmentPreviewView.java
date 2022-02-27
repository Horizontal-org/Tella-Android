package rs.readahead.washington.mobile.views.custom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectAttachmentMediaFilePresenter;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.activity.AudioPlayActivity;
import rs.readahead.washington.mobile.views.activity.PhotoViewerActivity;
import rs.readahead.washington.mobile.views.activity.VideoViewerActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;


public class CollectAttachmentPreviewView extends LinearLayout implements ICollectAttachmentMediaFilePresenterContract.IView {
    @BindView(R.id.thumbView)
    ImageView thumbView;
    /*@BindView(R.id.thumbGradient)
    View thumbGradient;*/
    @BindView(R.id.fileName)
    TextView fileName;
    @BindView(R.id.fileSize)
    TextView fileSize;
    @BindView(R.id.videoInfo)
    RelativeLayout videoInfo;
    @BindView(R.id.audioInfo)
    FrameLayout audioInfo;
    @BindView(R.id.videoDuration)
    TextView videoDuration;
    @BindView(R.id.audioDuration)
    TextView audioDuration;

    private VaultFile vaultFile;
    private final CollectAttachmentMediaFilePresenter presenter;
    private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;


    public CollectAttachmentPreviewView(Context context) {
        this(context, null);
    }

    public CollectAttachmentPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollectAttachmentPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.collect_attachemnt_preview_view, this);

        ButterKnife.bind(this);
        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);

        glide = Glide.with(getContext()).using(glideLoader);
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
            //thumbGradient.setVisibility(GONE);
            thumbView.setId(QuestionWidget.newUniqueId());
            thumbView.setOnClickListener(v -> showVideoViewerActivity());

           // showMediaFileInfo();
            loadThumbnail();
          //  videoDuration.setText(Util.getShortVideoDuration((int) (vaultFile.duration / 1000)));

            //audioInfo.setVisibility(GONE);
            //videoInfo.setVisibility(VISIBLE);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            //thumbGradient.setVisibility(VISIBLE);
            thumbView.setImageResource(R.drawable.ic_baseline_headset_24);
            thumbView.setScaleType(ImageView.ScaleType.CENTER);
            thumbView.setOnClickListener(v -> showAudioPlayActivity());

           // showMediaFileInfo();
            //audioDuration.setText(Util.getShortVideoDuration((int) (vaultFile.duration / 1000)));

            //audioInfo.setVisibility(VISIBLE);
            //videoInfo.setVisibility(GONE);
        } else if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
           // thumbGradient.setVisibility(GONE);
            thumbView.setId(QuestionWidget.newUniqueId());
            thumbView.setOnClickListener(v -> showPhotoViewerActivity());

            loadThumbnail();
           //showMediaFileInfo();

            audioInfo.setVisibility(GONE);
            videoInfo.setVisibility(GONE);
        } else if (MediaFile.INSTANCE.isTextFileType(vaultFile.mimeType)) {
            //thumbGradient.setVisibility(VISIBLE);
            thumbView.setImageResource(R.drawable.ic_baseline_assignment_24);
            thumbView.setScaleType(ImageView.ScaleType.CENTER);
            //thumbView.setOnClickListener(v -> showAudioPlayActivity());

           // showMediaFileInfo();
            //audioDuration.setText(Util.getShortVideoDuration((int) (vaultFile.duration / 1000)));

            audioInfo.setVisibility(GONE);
            videoInfo.setVisibility(GONE);
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
        thumbView.setImageResource(R.drawable.ic_error);
        Toast.makeText(getContext(), getResources().getText(R.string.collect_form_toast_fail_load_attachment), Toast.LENGTH_LONG).show();
        audioInfo.setVisibility(GONE);
        videoInfo.setVisibility(GONE);
    }

    private void loadThumbnail() {
        glide.load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(thumbView);
    }

    private void showMediaFileInfo() {
        fileName.setText(vaultFile.name);
        fileSize.setText(FileUtil.getFileSizeString(vaultFile.size));
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
