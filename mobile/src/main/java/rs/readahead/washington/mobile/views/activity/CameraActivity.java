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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
import java.util.List;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CaptureEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.databinding.ActivityCameraBinding;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ICameraPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CameraPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MetadataAttacher;
import rs.readahead.washington.mobile.mvp.presenter.TellaFileUploadSchedulePresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.VideoResolutionManager;
import rs.readahead.washington.mobile.views.custom.CameraCaptureButton;
import rs.readahead.washington.mobile.views.custom.CameraDurationTextView;
import rs.readahead.washington.mobile.views.custom.CameraFlashButton;
import rs.readahead.washington.mobile.views.custom.CameraGridButton;
import rs.readahead.washington.mobile.views.custom.CameraResolutionButton;
import rs.readahead.washington.mobile.views.custom.CameraSwitchButton;


public class CameraActivity extends MetadataActivity implements ICameraPresenterContract.IView, ITellaFileUploadSchedulePresenterContract.IView, IMetadataAttachPresenterContract.IView {
    public static final String MEDIA_FILE_KEY = "mfk";
    public static final String VAULT_CURRENT_ROOT_PARENT = "vcrf";
    private final static int CLICK_DELAY = 1200;
    private final static int CLICK_MODE_DELAY = 2000;
    public static String CAMERA_MODE = "cm";
    public static String INTENT_MODE = "im";
    public static String CAPTURE_WITH_AUTO_UPLOAD = "capture_with_auto_upload";
    CameraView cameraView;
    CameraGridButton gridButton;
    CameraSwitchButton switchButton;
    CameraFlashButton flashButton;
    CameraCaptureButton captureButton;
    CameraDurationTextView durationView;
    SeekBar mSeekBar;
    View videoLine;
    View photoLine;
    ImageView previewView;
    TextView photoModeText;
    TextView videoModeText;
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
    private String currentRootParent = null;
    private ActivityCameraBinding binding;
    private boolean captureWithAutoUpload = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        initListeners();

        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);

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

        if (getIntent().hasExtra(CAPTURE_WITH_AUTO_UPLOAD)) {
            captureWithAutoUpload = getIntent().getBooleanExtra(CAPTURE_WITH_AUTO_UPLOAD, false);
        }

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        if (captureWithAutoUpload) {
            scheduleFileUpload(capturedMediaFile);
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

    private void initListeners() {
        binding.close.setOnClickListener((view) -> closeCamera());
        captureButton.setOnClickListener((view) -> onCaptureClicked());
        binding.photoMode.setOnClickListener((view) -> onPhotoClicked());
        binding.videoMode.setOnClickListener((view) -> onVideoClicked());
        gridButton.setOnClickListener((view) -> onGridClicked());
        switchButton.setOnClickListener((view) -> onSwitchClicked());
        resolutionButton.setOnClickListener((view) -> chooseVideoResolution());
        previewView.setOnClickListener((view) -> onPreviewClicked());
    }

    private void closeCamera() {
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
            Glide.with(this).load(vaultFile.thumb)
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
        captureButton.setContentDescription(getContext().getString(R.string.Uwazi_WidgetMedia_Take_Photo));
        cameraView.setMode(Mode.PICTURE);
        mode = CameraMode.PHOTO;

        resetZoom();
        lastClickTime = System.currentTimeMillis();
    }

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
        captureButton.setContentDescription(getContext().getString(R.string.Uwazi_WidgetMedia_Take_Video));
        setVideoActive();
        mode = CameraMode.VIDEO;

        resetZoom();
        lastClickTime = System.currentTimeMillis();
    }

    void onGridClicked() {
        if (cameraView.getGrid() == Grid.DRAW_3X3) {
            cameraView.setGrid(Grid.OFF);
            gridButton.displayGridOff();
            gridButton.setContentDescription(getString(R.string.action_show_gridview));
        } else {
            cameraView.setGrid(Grid.DRAW_3X3);
            gridButton.displayGridOn();
            gridButton.setContentDescription(getString(R.string.action_hide_gridview));
        }
    }

    void onSwitchClicked() {
        if (cameraView.getFacing() == Facing.BACK) {
            switchCamera(Facing.FRONT, R.string.action_switch_to_back_camera);
        } else {
            switchCamera(Facing.BACK, R.string.action_switch_to_front_camera);
        }
    }

    private void switchCamera(Facing facing, int contentDescriptionResId) {
        cameraView.setFacing(facing);
        switchButton.displayCamera(facing);
        switchButton.setContentDescription(getString(contentDescriptionResId));
    }

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
        displayVideoCaptureButton();
        durationView.stop();
        presenter.addMp4Video(video, currentRootParent);
    }

    /* handle display settings for the video capture button.*/
    private void displayVideoCaptureButton() {
        captureButton.displayVideoButton();
        captureButton.setContentDescription(getContext().getString(R.string.Uwazi_WidgetMedia_Take_Video));
    }

    private void setupCameraView() {
        if (mode == CameraMode.PHOTO) {
            setCameraMode(Mode.PICTURE);
            displayPhotoCaptureButton();
        } else {
            setCameraMode(Mode.VIDEO);
            displayVideoCaptureButton();
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
                FirebaseCrashlytics.getInstance().recordException(exception);
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

    /* handle the behavior of setting the camera mode.*/
    private void setCameraMode(Mode mode) {
        cameraView.setMode(mode);
    }

    /* handle display settings for the photo capture button.*/
    private void displayPhotoCaptureButton() {
        captureButton.displayPhotoButton();
        captureButton.setContentDescription(getContext().getString(R.string.Uwazi_WidgetMedia_Take_Photo));
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
            gridButton.setContentDescription(getString(R.string.action_hide_gridview));
        } else {
            gridButton.displayGridOff();
            gridButton.setContentDescription(getString(R.string.action_show_gridview));
        }
    }

    private void setupCameraSwitchButton() {
        if (cameraView.getFacing() == Facing.FRONT) {
            switchCamera(Facing.FRONT, R.string.action_switch_to_back_camera);
        } else {
            switchCamera(Facing.BACK, R.string.action_switch_to_front_camera);
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
            flashButton.setContentDescription(getString(R.string.action_enable_flash));
        } else {
            flashButton.displayFlashOn();
            flashButton.setContentDescription(getString(R.string.action_disable_flash));
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
        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

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
            uploadPresenter.scheduleUploadReportFiles(vaultFile, Preferences.getAutoUploadServerId());
        }
    }

    private void setupShutterSound() {
        cameraView.setPlaySounds(!Preferences.isShutterMute());
    }

    private void initView() {
        cameraView = binding.camera;
        gridButton = binding.gridButton;
        switchButton = binding.switchButton;
        flashButton = binding.flashButton;
        captureButton = binding.captureButton;
        durationView = binding.durationView;
        mSeekBar = binding.cameraZoom;
        videoLine = binding.videoLine;
        photoLine = binding.photoLine;
        previewView = binding.previewImage;
        photoModeText = binding.photoText;
        videoModeText = binding.videoText;
        resolutionButton = binding.resolutionButton;
    }

    public enum CameraMode {
        PHOTO, VIDEO
    }

    public enum IntentMode {
        COLLECT, RETURN, STAND, ODK
    }
}