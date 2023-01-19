package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILE_KEY;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.google.gson.Gson;
import com.hzontal.tella_vault.VaultFile;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.size.SizeSelector;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CaptureEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.mvp.contract.ICameraPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CameraPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadSchedulePresenter;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.VideoResolutionManager;
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton;
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView;
import rs.readahead.washington.mobile.views.custom.CameraFlashButton;
import rs.readahead.washington.mobile.views.custom.CameraGridButton;
import rs.readahead.washington.mobile.views.custom.CameraResolutionButton;
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton;
import timber.log.Timber;


public class CameraActivity extends MetadataActivity implements
        ICameraPresenterContract.IView,
        ITellaFileUploadSchedulePresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    public static final String MEDIA_FILE_KEY = "mfk";
    public static final String VAULT_CURRENT_ROOT_PARENT = "vcrf";
    private final static int CLICK_DELAY = 1200;
    private final static int CLICK_MODE_DELAY = 2000;
    public static String CAMERA_MODE = "cm";
    public static String INTENT_MODE = "im";
    @BindView(R.id.camera)
    CameraView cameraView;
    @BindView(R.id.gridButton)
    CameraGridButton gridButton;
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
    private TellaFileUploadSchedulePresenter uploadPresenter;
    private MetadataAttacher metadataAttacher;
    private CameraMode mode;
    private boolean modeLocked;
    private IntentMode intentMode;
    private boolean videoRecording;
    private ProgressDialog progressDialog;
    private OrientationEventListener mOrientationEventListener;
    private int zoomLevel = 0;
    private VaultFile capturedMediaFile;
    private AlertDialog videoQualityDialog;
    private VideoResolutionManager videoResolutionManager;
    private long lastClickTime = System.currentTimeMillis();
    private RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;
    private String currentRootParent = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
        ButterKnife.bind(this);

        presenter = new CameraPresenter(this);
        uploadPresenter = new TellaFileUploadSchedulePresenter(this);
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

        if (getIntent().hasExtra(VAULT_CURRENT_ROOT_PARENT)) {
            currentRootParent = getIntent().getStringExtra(VAULT_CURRENT_ROOT_PARENT);
        }

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);

        setupCameraView();
        setupCameraModeButton();
        setupImagePreview();
        setupShutterSound();
        checkLocationSettings(C.START_CAMERA_CAPTURE, () -> {
        });
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            maybeChangeTemporaryTimeout();
        }


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
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    @Override
    public void onAddingStart() {
        progressDialog = DialogsUtil.showLightProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting));
        if (Preferences.isShutterMute()) {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }
    }

    @Override
    public void onAddingEnd() {
        hideProgressDialog();
        showToast(R.string.gallery_toast_file_encrypted);
    }

    @Override
    public void onAddSuccess(VaultFile bundle) {
        capturedMediaFile = bundle;
        if (intentMode != IntentMode.COLLECT) {
            previewView.setVisibility(View.VISIBLE);
            Glide.with(this).load(bundle.thumb).into(previewView);
        }

        if (!Preferences.isAnonymousMode()) {
            attachMediaFileMetadata(capturedMediaFile, metadataAttacher);
        } else {
            returnIntent(bundle);
        }

        MyApplication.bus().post(new CaptureEvent());
    }

    @Override
    public void onAddError(Throwable error) {
        showToast(R.string.gallery_toast_fail_saving_file);
    }

    @Override
    public void onMetadataAttached(VaultFile vaultFile) {
        returnIntent(vaultFile);

        //scheduleFileUpload(capturedMediaFile);
    }

    private void returnIntent(VaultFile vaultFile) {
        Intent data = new Intent();
        if (intentMode == IntentMode.ODK) {
            capturedMediaFile.metadata = vaultFile.metadata;
            data.putExtra(MEDIA_FILE_KEY, capturedMediaFile);
            setResult(RESULT_OK, data);
            finish();
        } else if (intentMode == IntentMode.COLLECT) {
            capturedMediaFile.metadata = vaultFile.metadata;
            List<String> list = new ArrayList<>();
            list.add(vaultFile.id);
            data.putExtra(VAULT_FILE_KEY, new Gson().toJson(list));
            setResult(RESULT_OK, data);
            finish();
        } else {
            data.putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile.metadata);
            setResult(RESULT_OK, data);
        }
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
        gridButton.rotateView(rotation);
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
    public void onLastMediaFileSuccess(VaultFile vaultFile) {
        if (intentMode != IntentMode.COLLECT) {
            previewView.setVisibility(View.VISIBLE);
            glide.load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(previewView);
        }
    }

    @Override
    public void onLastMediaFileError(Throwable throwable) {
        if (intentMode != IntentMode.COLLECT || intentMode == IntentMode.ODK) {
            previewView.setVisibility(View.GONE);
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

    @Override
    public void onGetMediaFilesSuccess(List<VaultFile> mediaFiles) {

    }

    @Override
    public void onGetMediaFilesError(Throwable error) {

    }

    @OnClick(R.id.captureButton)
    void onCaptureClicked() {
        if (Preferences.isShutterMute()) {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
        if (cameraView.getMode() == Mode.PICTURE) {
            cameraView.takePicture();
        } else {
            gridButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            switchButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);
            resolutionButton.setVisibility(videoRecording ? View.VISIBLE : View.GONE);

            if (videoRecording) {
                if (System.currentTimeMillis() - lastClickTime >= CLICK_DELAY) {
                    cameraView.stopVideo();
                    videoRecording = false;
                    gridButton.setVisibility(View.VISIBLE);
                    switchButton.setVisibility(View.VISIBLE);
                    resolutionButton.setVisibility(View.VISIBLE);
                }
            } else {
                setVideoQuality();
                lastClickTime = System.currentTimeMillis();
                cameraView.takeVideo(MediaFileHandler.getTempFile());
                captureButton.displayStopVideo();
                durationView.start();
                videoRecording = true;
                gridButton.setVisibility(View.GONE);
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

    @OnClick(R.id.gridButton)
    void onGridClicked() {
        if (cameraView.getGrid() == Grid.DRAW_3X3) {
            cameraView.setGrid(Grid.OFF);
            gridButton.displayGridOff();
        } else {
            cameraView.setGrid(Grid.DRAW_3X3);
            gridButton.displayGridOn();
        }
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.PHOTO_VIDEO_FILTER, "filter");
        startActivity(intent);
        finish();
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
        presenter.addMp4Video(video, currentRootParent);
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
                presenter.addJpegPhoto(result.getData(), currentRootParent);
            }

            @Override
            public void onVideoTaken(@NotNull VideoResult result) {
                showConfirmVideoView(result.getFile());
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                Timber.e(exception);
            }

            @Override
            public void onCameraOpened(@NotNull CameraOptions options) {
                if (options.supports(Grid.DRAW_3X3)) {
                    gridButton.setVisibility(View.VISIBLE);
                    setUpCameraGridButton();
                } else {
                    gridButton.setVisibility(View.GONE);
                }

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

    private void setUpCameraGridButton() {
        if (cameraView.getGrid() == Grid.DRAW_3X3) {
            gridButton.displayGridOn();
        } else {
            gridButton.displayGridOff();
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
        if (intentMode == IntentMode.COLLECT || intentMode == IntentMode.ODK) {
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

    private void scheduleFileUpload(VaultFile vaultFile) {
        if (Preferences.isAutoUploadEnabled()) {
            List<VaultFile> upload = Collections.singletonList(vaultFile);
            uploadPresenter.scheduleUploadMediaFiles(upload);
        } else {
            onMediaFilesUploadScheduled();
        }
    }

    private void setupShutterSound() {
        cameraView.setPlaySounds(!Preferences.isShutterMute());
    }

    public enum CameraMode {
        PHOTO,
        VIDEO
    }

    public enum IntentMode {
        COLLECT,
        RETURN,
        STAND,
        ODK
    }
}