package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hzontal.tella_vault.VaultFile;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.mvp.contract.ICollectCreateFormControllerContract;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.HomeScreenPresenter;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.views.custom.HomeScreenGradient;


@RuntimePermissions
public class MainActivity extends MetadataActivity implements
        IMetadataAttachPresenterContract.IView,
        IHomeScreenPresenterContract.IView,
        ICollectCreateFormControllerContract.IView {
    @BindView(R.id.home_screen_gradient)
    HomeScreenGradient homeScreenGradient;
    @BindView(R.id.nav_bar_holder)
    LinearLayout navBarHolder;
    @BindView(R.id.background)
    View background;
    @BindView(R.id.main_container)
    View root;
    private boolean mExit = false;
    private Handler handler;
    private EventCompositeDisposable disposables;
    private AlertDialog alertDialog;
    private HomeScreenPresenter homeScreenPresenter;
    private ProgressDialog progressDialog;
    private OrientationEventListener mOrientationEventListener;
    private BottomNavigationView btmNavMain;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private long numOfCollectServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);

        //  setupToolbar();
        setupNavigation();
        handler = new Handler();

        homeScreenPresenter = new HomeScreenPresenter(this);

        initSetup();

        // todo: check this..
        //SafetyNetCheck.setApiKey(getString(R.string.share_in_report));
    }

    private void initSetup() {
        //handleOrbot();
        //setupPanicSeek();

        setOrientationListener();

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(LocaleChangedEvent event) {
                recreate();
            }
        });
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_host);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();
        btmNavMain = findViewById(R.id.btm_nav_main);
        appBarConfiguration =
                new AppBarConfiguration.Builder(R.id.homeScreen, R.id.cameraScreen, R.id.reportsScreen, R.id.micScreen, R.id.formScreen).build();
        NavigationUI.setupWithNavController(btmNavMain, navController);
        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == (R.id.homeScreen) || navDestination.getId() == R.id.formScreen || navDestination.getId() == R.id.micScreen) {
                showBottomNavigation();
            } else {
                hideBottomNavigation();
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @NeedsPermission(Manifest.permission.CAMERA)
    @OnClick(R.id.camera_tools_container)
    void onContainerClicked() {
        if (Preferences.isAnonymousMode()) {
            MainActivityPermissionsDispatcher.switchToCameraModeAnonymousWithPermissionCheck(this);
        } else {
            MainActivityPermissionsDispatcher.switchToCameraModeLocationWithPermissionCheck(this);
        }
    }

    //@OnClick(R.id.microphone)
    void onMicrophoneClicked() {
        if (Preferences.isAnonymousMode()) {
            startAudioRecorderActivityAnonymous();
        } else {
            MainActivityPermissionsDispatcher.startAudioRecorderActivityLocationWithPermissionCheck(this);
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void enableCamera() {
        recreate(); // we have permissions, recreate activity to show preview..
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void switchToCameraModeLocation() {
        checkLocationSettings(C.START_CAMERA_CAPTURE, this::switchToCameraModeWithLocationChecked);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    public void switchToCameraModeAnonymous() {
        switchToCameraMode();
    }

    private void switchToCameraModeWithLocationChecked() {
        startLocationMetadataListening();
        switchToCameraMode();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startAudioRecorderActivityLocation() {
        checkLocationSettings(C.START_AUDIO_RECORD, this::startAudioRecordActivityWithLocationChecked);
    }

    private void startAudioRecorderActivityAnonymous() {
        startAudioRecordActivityWithLocationChecked();
    }

    private void startAudioRecordActivityWithLocationChecked() {
        Intent intent = new Intent(this, AudioRecordActivity2.class);
        intent.putExtra(AudioRecordActivity2.RECORDER_MODE, AudioRecordActivity2.Mode.STAND.name());
        startActivityForResult(intent, C.RECORDED_AUDIO);
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.permission_dialog_expl_GPS));
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showCameraRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_camera));
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_camera_mic));
    }

    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showLocationCameraAndAudioRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_camera_mic));
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void onCameraPermissionDenied() {
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioPermissionDenied() {
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onLocationCameraAndAudioPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startAudioRecordActivityWithLocationChecked();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void onCameraNeverAskAgain() {
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraAndAudioNeverAskAgain() {
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onLocationCameraAndAudioNeverAskAgain() {
    }

    private void startCollectActivity() {
        startActivity(new Intent(MainActivity.this, CollectMainActivity.class));
    }

    private void startSettingsActivity() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    private void showGallery(boolean animated) {
        if (Preferences.isAnonymousMode()) {
            startGalleryActivity(animated);
        } else {
            MainActivityPermissionsDispatcher.startGalleryActivityWithPermissionCheck(this, animated);
        }
    }

    private void startUploadsActivity() {
        startActivity(new Intent(MainActivity.this, UploadsActivity.class));
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startGalleryActivity(boolean animated) {
        startActivity(new Intent(MainActivity.this, GalleryActivity.class)
                .putExtra(GalleryActivity.GALLERY_ANIMATED, animated));
    }

    private boolean isLocationSettingsRequestCode(int requestCode) {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            return; // user canceled evidence acquiring
        }
        List<Fragment> fragments = Objects.requireNonNull(getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

        switch (requestCode) {
            case C.START_CAMERA_CAPTURE:
                switchToCameraModeWithLocationChecked();
                break;

            case C.START_AUDIO_RECORD:
                startAudioRecordActivityWithLocationChecked();
                break;

            case C.CAMERA_CAPTURE:
            case C.RECORDED_AUDIO:
                // everything is done already..
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        // if (maybeCloseCamera()) return;
        if (!checkIfShouldExit()) return;
        closeApp();
    }

    private void closeApp() {
        finish();
        lockApp();
    }

    private boolean checkIfShouldExit() {
        if (!mExit) {
            mExit = true;
            showToast(R.string.home_toast_back_exit);
            handler.postDelayed(() -> mExit = false, 3 * 1000);
            return false;
        }
        return true;
    }

    private void lockApp() {
        if (!isLocked()) {
            MyApplication.resetKeys();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (disposables != null) {
            disposables.dispose();
        }

        stopPresenter();
        hideProgressDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        btmNavMain.setSelectedItemId(R.id.home);

        homeScreenPresenter.countCollectServers();

        startLocationMetadataListening();

        mOrientationEventListener.enable();

        // setOfflineMenuIcon(toolbar.getMenu().findItem(R.id.nav_offline_mode), Preferences.isOfflineMode());
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationMetadataListening();

        mOrientationEventListener.disable();
    }

    @Override
    protected void onStop() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onStop();
    }

    @Override
    public void onMetadataAttached(VaultFile vaultFile) {
        Intent data = new Intent();
        data.putExtra(C.CAPTURED_MEDIA_FILE_ID, vaultFile.id);

        setResult(RESULT_OK, data);
    }

    @Override
    public void onMetadataAttachError(Throwable throwable) {
        // onAddError(throwable);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startCollectFormEntryActivity() {
        startActivity(new Intent(this, CollectFormEntryActivity.class));
    }

    @Override
    public void onFormControllerCreated(FormController formController) {
        if (Preferences.isAnonymousMode()) {
            startCollectFormEntryActivity(); // no need to check for permissions, as location won't be turned on
        } else {
            MainActivityPermissionsDispatcher.startCollectFormEntryActivityWithPermissionCheck(this);
        }
    }

    @Override
    public void onFormControllerError(Throwable error) {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCountTUServersEnded(Long num) {
        setupButtonsTab(num);
    }

    @Override
    public void onCountTUServersFailed(Throwable throwable) {
    }

    @Override
    public void onCountCollectServersEnded(Long num) {
        this.numOfCollectServers = num;
        homeScreenPresenter.countTUServers();
    }

    @Override
    public void onCountCollectServersFailed(Throwable throwable) {

    }

    private void stopPresenter() {
        if (homeScreenPresenter != null) {
            homeScreenPresenter.destroy();
            homeScreenPresenter = null;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void switchToCameraMode() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.STAND.name());
        startActivity(intent);
    }

    private void setOrientationListener() {
        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                //if (!isInCameraMode) return;
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
                // handle rotation for tablets;
            }
        };
    }


    private void setupButtonsTab(long numOfTUS) {
        LinearLayout navbar;
        LayoutInflater inflater = LayoutInflater.from(this);

        if (numOfCollectServers > 0 && numOfTUS > 0) {
            navbar = (LinearLayout) inflater.inflate(R.layout.main_navigation_table_layout, null);
        } else {
            navbar = (LinearLayout) inflater.inflate(R.layout.main_navigation_row_layout, null);
        }

        View buttonCollect = navbar.findViewById(R.id.tab_button_collect);
        View buttonRecord = navbar.findViewById(R.id.tab_button_record);
        View buttonUploads = navbar.findViewById(R.id.tab_button_uploads);
        View buttonGallery = navbar.findViewById(R.id.tab_button_gallery);

        buttonGallery.setOnClickListener(v -> showGallery(false));
        buttonCollect.setOnClickListener(v -> startCollectActivity());
        buttonRecord.setOnClickListener(v -> onMicrophoneClicked());
        buttonUploads.setOnClickListener(v -> startUploadsActivity());

        navBarHolder.removeAllViews();
        navBarHolder.addView(navbar);

        if (numOfCollectServers == 0 && numOfTUS == 0) { //row layout of 2
            buttonCollect.setVisibility(View.GONE);
            buttonUploads.setVisibility(View.GONE);
            return;
        }

        if (numOfCollectServers > 0 && numOfTUS == 0) {  //nav layout of 3 with collect
            buttonGallery.setBackground(getResources().getDrawable(R.drawable.central_button_background));
            buttonCollect.setBackground(getResources().getDrawable(R.drawable.round_left_button_background));
            buttonUploads.setVisibility(View.GONE);
            return;
        }

        if (numOfCollectServers == 0 && numOfTUS > 0) { //nav layout of 3 with uploads
            buttonCollect.setVisibility(View.GONE);
            buttonRecord.setBackground(getResources().getDrawable(R.drawable.central_button_background));
            buttonUploads.setBackground(getResources().getDrawable(R.drawable.round_right_button_background));
        }
    }

    public void hideBottomNavigation() {
        btmNavMain.setVisibility(View.GONE);
    }

    public void showBottomNavigation() {
        btmNavMain.setVisibility(View.VISIBLE);
    }
    public void enableMoveMode(Boolean isEnabled){
        root.setBackgroundColor(R.color.prussian_blue);
       /* if (!isEnabled){

        }else{
            root.setBackgroundColor(R.color.space_cadet);
        }*/
    }
}

