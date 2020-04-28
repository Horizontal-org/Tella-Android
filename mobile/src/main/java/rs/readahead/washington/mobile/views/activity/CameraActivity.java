package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.size.SizeSelector;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.TempMediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.ICameraPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CameraPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadPresenter;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.VideoResolutionManager;
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton;
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView;
import rs.readahead.washington.mobile.views.custom.CameraFlashButton;
import rs.readahead.washington.mobile.views.custom.CameraResolutionButton;
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton;


public class CameraActivity extends MetadataActivity implements
        ICameraPresenterContract.IView,
        ITellaFileUploadPresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    public static String CAMERA_MODE = "cm";
    public static String INTENT_MODE = "im";
    public static final String MEDIA_FILE_KEY = "mfk";

    @BindView(R.id.camera)
    CameraView cameraView;
    @BindView(R.id.switchButton)
    CameraSwitchButton switchButton;
    @BindView(R.id.flashButton)
    CameraFlashButton flashButton;
    @BindView(R.id.captureButton)
    CameraCaptureButton captureButton;
    @BindView(R.id.durationView)
    CameraDurationTextView durationView;
    @BindView(R.id.camera_zoom)
    SeekBar mSeekBar;
    @BindView(R.id.video_line)
    View videoLine;
    @BindView(R.id.photo_line)
    View photoLine;
    @BindView(R.id.preview_image)
    ImageView previewView;
    @BindView(R.id.photo_text)
    TextView photoModeText;
    @BindView(R.id.video_text)
    TextView videoModeText;
    @BindView(R.id.resolutionButton)
    CameraResolutionButton resolutionButton;

    private CameraPresenter presenter;
    private TellaFileUploadPresenter uploadPresenter;
    private MetadataAttacher metadataAttacher;
    private CameraMode mode;
    private boolean modeLocked;
    private IntentMode intentMode;
    private boolean videoRecording;
    private ProgressDialog progressDialog;
    private OrientationEventListener mOrientationEventListener;
    private int zoomLevel = 0;
    private MediaFile capturedMediaFile;
    private AlertDialog videoQualityDialog;
    private VideoResolutionManager videoResolutionManager;

    public enum CameraMode {
        PHOTO,
        VIDEO
    }

    public enum IntentMode {
        COLLECT,
        RETURN,
        STAND
    }

    private final static int CLICK_DELAY = 1200;
    private final static int CLICK_MODE_DELAY = 2000;
    private long lastClickTime = System.currentTimeMillis();

    private RequestManager.ImageModelRequest<MediaFileLoaderModel> glide;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        presenter = new CameraPresenter(this);
        uploadPresenter = new TellaFileUploadPresenter(this);
        metadataAttacher = new MetadataAttacher(this);

        mode = CameraMode.PHOTO;
        if (getIntent().hasExtra(CAMERA_MODE)) {
            mode = CameraMode.valueOf(getIntent().getStringExtra(CAMERA_MODE));
            modeLocked = true;
        }

        intentMode = IntentMode.RETURN;
        if (getIntent().hasExtra(INTENT_MODE)) {
            intentMode = IntentMode.valueOf(getIntent().getStringExtra(INTENT_MODE));
        }

        CacheWordDataSource cacheWordDataSource = new CacheWordDataSource(getContext());
        MediaFileHandler mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
        MediaFileUrlLoader glideLoader = new MediaFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);

        setupCameraView();
        setupCameraModeButton();
        setupImagePreview();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mOrientationEventListener.enable();

        startLocationMetadataListening();

        cameraView.open();
        setVideoQuality();

        mSeekBar.setProgress(zoomLevel);
        setCameraZoom();

        presenter.getLastMediaFile();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationMetadataListening();

        mOrientationEventListener.disable();

        if (videoRecording) {
            captureButton.performClick();
        }

        cameraView.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPresenter();
        hideProgressDialog();
        hideVideoResolutionDialog();
        cameraView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (maybeStopVideoRecording()) return;
        super.onBackPressed();
    }

    @Override
    public void onAddingStart() {
        progressDialog = DialogsUtil.showLightProgressDialog(this, getString(R.string.ra_import_media_progress));
    }

    @Override
    public void onAddingEnd() {
        hideProgressDialog();
        showToast(R.string.ra_file_encrypted);
    }

    @Override
    public void onAddSuccess(MediaFileBundle bundle) {
        capturedMediaFile = bundle.getMediaFile();
        if (intentMode != IntentMode.COLLECT) {
            Glide.with(this).load(bundle.getMediaFileThumbnailData().getData()).into(previewView);
        }
        attachMediaFileMetadata(capturedMediaFile, metadataAttacher);
    }

    @Override
    public void onAddError(Throwable error) {
        showToast(R.string.ra_capture_error);
    }

    @Override
    public void onMetadataAttached(long mediaFileId, @Nullable Metadata metadata) {
        Intent data = new Intent();
        if (intentMode == IntentMode.COLLECT) {
            capturedMediaFile.setMetadata(metadata);
            data.putExtra(MEDIA_FILE_KEY, capturedMediaFile);
        } else {
            data.putExtra(C.CAPTURED_MEDIA_FILE_ID, mediaFileId);
        }
        setResult(RESULT_OK, data);

        scheduleFileUpload(capturedMediaFile);
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        onAddError(throwable);
    }

    @OnClick(R.id.close)
    void closeCamera() {
        onBackPressed();
    }

    @Override
    public void rotateViews(int rotation) {
        switchButton.rotateView(rotation);
        flashButton.rotateView(rotation);
        durationView.rotateView(rotation);
        captureButton.rotateView(rotation);
        if (mode != CameraMode.PHOTO) {
            resolutionButton.rotateView(rotation);
        }
        if (intentMode != IntentMode.COLLECT) {
            previewView.animate().rotation(rotation).start();
        }
    }

    @Override
    public void onLastMediaFileSuccess(MediaFile mediaFile) {
        if (intentMode != IntentMode.COLLECT) {
            glide.load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(previewView);
        }
    }

    @Override
    public void onLastMediaFileError(Throwable throwable) {
        if (intentMode != IntentMode.COLLECT) {
            previewView.setImageResource(R.drawable.white);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onMediaFilesUploadScheduled() {
        if (intentMode != IntentMode.STAND) {
            finish();
        }
    }

    @Override
    public void onMediaFilesUploadScheduleError(Throwable throwable) {

    }

    @OnClick(R.id.captureButton)
    void onCaptureClicked() {
        if (cameraView.getMode() == Mode.PICTURE) {
            cameraView.takePicture();
        } else {

            switchButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            resolutionButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            if (videoRecording) {
                if (System.currentTimeMillis() - lastClickTime >= CLICK_DELAY) {
                    cameraView.stopVideo();
                    videoRecording = false;
                    switchButton.setVisibility(View.VISIBLE);
                    resolutionButton.setVisibility(View.VISIBLE);
                }
            } else {
                setVideoQuality();
                lastClickTime = System.currentTimeMillis();
                TempMediaFile tmp = TempMediaFile.newMp4();
                File file = MediaFileHandler.getTempFile(this, tmp);
                cameraView.takeVideo(file);
                captureButton.displayStopVideo();
                durationView.start();
                videoRecording = true;
                switchButton.setVisibility(View.GONE);
                resolutionButton.setVisibility(View.GONE);
            }
        }
    }

    @OnClick(R.id.photo_mode)
    void onPhotoClicked() {
        if (modeLocked) {
            return;
        }

        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) {
            return;
        }

        if (cameraView.getMode() == Mode.PICTURE) {
            return;
        }

        if (cameraView.getFlash() == Flash.TORCH) {
            cameraView.setFlash(Flash.AUTO);
        }

        setPhotoActive();
        captureButton.displayPhotoButton();
        cameraView.setMode(Mode.PICTURE);
        mode = CameraMode.PHOTO;

        resetZoom();
        lastClickTime = System.currentTimeMillis();
    }

    @OnClick(R.id.video_mode)
    void onVideoClicked() {
        if (modeLocked) {
            return;
        }

        if (System.currentTimeMillis() - lastClickTime < CLICK_MODE_DELAY) {
            return;
        }

        if (cameraView.getMode() == Mode.VIDEO) {
            return;
        }

        cameraView.setMode(Mode.VIDEO);
        turnFlashDown();
        captureButton.displayVideoButton();
        setVideoActive();
        mode = CameraMode.VIDEO;

        resetZoom();
        lastClickTime = System.currentTimeMillis();
    }

    @OnClick(R.id.switchButton)
    void onSwitchClicked() {
        if (cameraView.getFacing() == Facing.BACK) {
            cameraView.setFacing(Facing.FRONT);
            switchButton.displayFrontCamera();
        } else {
            cameraView.setFacing(Facing.BACK);
            switchButton.displayBackCamera();
        }
    }

    @OnClick(R.id.preview_image)
    void onPreviewClicked() {
        startActivity(new Intent(this, GalleryActivity.class));
    }

    private void resetZoom() {
        zoomLevel = 0;
        mSeekBar.setProgress(0);
        setCameraZoom();
    }

    @OnClick(R.id.resolutionButton)
    public void chooseVideoResolution() {
        if (videoResolutionManager != null) {
            videoQualityDialog = DialogsUtil.showVideoResolutionDialog(this, this::setVideoSize, videoResolutionManager);
        }
    }

    private void setCameraZoom() {
        cameraView.setZoom((float) zoomLevel / 100);
    }

    private boolean maybeStopVideoRecording() {
        if (videoRecording) {
            captureButton.performClick();
            return true;
        }

        return false;
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void showConfirmVideoView(final File video) {
        captureButton.displayVideoButton();
        durationView.stop();
        presenter.addMp4Video(video);
    }

    private void setupCameraView() {
        if (mode == CameraMode.PHOTO) {
            cameraView.setMode(Mode.PICTURE);
            captureButton.displayPhotoButton();
        } else {
            cameraView.setMode(Mode.VIDEO);
            captureButton.displayVideoButton();
        }

        //cameraView.setEnabled(PermissionUtil.checkPermission(this, Manifest.permission.CAMERA));
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);

        setOrientationListener();

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NotNull PictureResult result) {
                presenter.addJpegPhoto(result.getData());
            }

            @Override
            public void onVideoTaken(@NotNull VideoResult result) {
                showConfirmVideoView(result.getFile());
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                Crashlytics.logException(exception);
            }

            @Override
            public void onCameraOpened(@NotNull CameraOptions options) {
                if (options.getSupportedFacing().size() < 2) {
                    switchButton.setVisibility(View.GONE);
                } else {
                    switchButton.setVisibility(View.VISIBLE);
                    setupCameraSwitchButton();
                }

                if (options.getSupportedFlash().size() < 2) {
                    flashButton.setVisibility(View.INVISIBLE);
                } else {
                    flashButton.setVisibility(View.VISIBLE);
                    setupCameraFlashButton(options.getSupportedFlash());
                }

                if (options.getSupportedVideoSizes().size() > 0) {
                    videoResolutionManager = new VideoResolutionManager(options.getSupportedVideoSizes());
                }
                // options object has info
                super.onCameraOpened(options);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                zoomLevel = i;
                setCameraZoom();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupCameraModeButton() {
        if (cameraView.getMode() == Mode.PICTURE) {
            setPhotoActive();
        } else {
            setVideoActive();
        }
    }

    private void setupCameraSwitchButton() {
        if (cameraView.getFacing() == Facing.FRONT) {
            switchButton.displayFrontCamera();
        } else {
            switchButton.displayBackCamera();
        }
    }

    private void setupImagePreview() {
        if (intentMode == IntentMode.COLLECT) {
            previewView.setVisibility(View.GONE);
        }
    }

    private void setupCameraFlashButton(final Collection<Flash> supported) {
        if (cameraView.getFlash() == Flash.AUTO) {
            flashButton.displayFlashAuto();
        } else if (cameraView.getFlash() == Flash.OFF) {
            flashButton.displayFlashOff();
        } else {
            flashButton.displayFlashOn();
        }

        flashButton.setOnClickListener(view -> {
            if (cameraView.getMode() == Mode.VIDEO) {
                if (cameraView.getFlash() == Flash.OFF && supported.contains(Flash.TORCH)) {
                    flashButton.displayFlashOn();
                    cameraView.setFlash(Flash.TORCH);
                } else {
                    turnFlashDown();
                }
            } else {
                if (cameraView.getFlash() == Flash.ON || cameraView.getFlash() == Flash.TORCH) {
                    turnFlashDown();
                } else if (cameraView.getFlash() == Flash.OFF && supported.contains(Flash.AUTO)) {
                    flashButton.displayFlashAuto();
                    cameraView.setFlash(Flash.AUTO);
                } else {
                    flashButton.displayFlashOn();
                    cameraView.setFlash(Flash.ON);
                }
            }
        });
    }

    private void turnFlashDown() {
        flashButton.displayFlashOff();
        cameraView.setFlash(Flash.OFF);
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void setOrientationListener() {
        mOrientationEventListener = new OrientationEventListener(
                this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                    presenter.handleRotation(orientation);
                }
            }
        };
    }

    private void setPhotoActive() {
        videoLine.setVisibility(View.GONE);
        photoLine.setVisibility(View.VISIBLE);
        photoModeText.setAlpha(1f);
        videoModeText.setAlpha(modeLocked ? 0.1f : 0.5f);
        resolutionButton.setVisibility(View.GONE);
    }

    private void setVideoActive() {
        videoLine.setVisibility(View.VISIBLE);
        photoLine.setVisibility(View.GONE);
        videoModeText.setAlpha(1);
        photoModeText.setAlpha(modeLocked ? 0.1f : 0.5f);
        if (videoResolutionManager != null) {
            resolutionButton.setVisibility(View.VISIBLE);
        }
    }

    private void hideVideoResolutionDialog() {
        if (videoQualityDialog != null) {
            videoQualityDialog.dismiss();
            videoQualityDialog = null;
        }
    }

    private void setVideoQuality() {
        if (cameraView != null && videoResolutionManager != null) {
            cameraView.setVideoSize(videoResolutionManager.getVideoSize());
        }
    }

    private void setVideoSize(SizeSelector videoSize) {
        if (cameraView != null) {
            cameraView.setVideoSize(videoSize);
            cameraView.close();
            cameraView.open();
        }
    }

    private void scheduleFileUpload(MediaFile mediaFile) {
        if (Preferences.isAutoUploadEnabled()) {
            List<MediaFile> upload = Collections.singletonList(mediaFile);
            uploadPresenter.scheduleUploadMediaFiles(upload);
        } else {
            onMediaFilesUploadScheduled();
        }
    }
}