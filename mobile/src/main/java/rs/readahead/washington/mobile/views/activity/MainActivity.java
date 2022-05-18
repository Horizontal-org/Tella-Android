package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelectorKt.VAULT_FILE_KEY;
import static rs.readahead.washington.mobile.views.fragment.vault.home.HomeVaultFragmentKt.VAULT_FILTER;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.OrientationEventListener;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMediaImportPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IMetadataAttachPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.HomeScreenPresenter;
import rs.readahead.washington.mobile.mvp.presenter.MediaImportPresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.CleanInsightUtils;
import rs.readahead.washington.mobile.views.fragment.uwazi.SubmittedPreviewFragment;
import rs.readahead.washington.mobile.views.fragment.uwazi.download.DownloadedTemplatesFragment;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryFragment;
import rs.readahead.washington.mobile.views.fragment.uwazi.send.UwaziSendFragment;
import rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsFragment;

import com.google.gson.Gson;

import timber.log.Timber;


@RuntimePermissions
public class MainActivity extends MetadataActivity implements
        IHomeScreenPresenterContract.IView,
        IMediaImportPresenterContract.IView,
        IMetadataAttachPresenterContract.IView {
    public static final String PHOTO_VIDEO_FILTER = "gallery_filter";
    @BindView(R.id.main_container)
    View root;
    private boolean mExit = false;
    private Handler handler;
    private EventCompositeDisposable disposables;
    private HomeScreenPresenter homeScreenPresenter;
    private MediaImportPresenter mediaImportPresenter;
    private ProgressDialog progressDialog;
    private OrientationEventListener mOrientationEventListener;
    private BottomNavigationView btmNavMain;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);

        //  setupToolbar();
        setupNavigation();
        handler = new Handler();
        homeScreenPresenter = new HomeScreenPresenter(this);
        mediaImportPresenter = new MediaImportPresenter(this);
        initSetup();
        // todo: check this..
        //SafetyNetCheck.setApiKey(getString(R.string.share_in_report));

        if (getIntent().hasExtra(PHOTO_VIDEO_FILTER)) {
            Bundle bundle = new Bundle();
            bundle.putString(VAULT_FILTER, FilterType.PHOTO_VIDEO.name());
            navController.navigate(R.id.action_homeScreen_to_attachments_screen, bundle);
        }
    }

    private void initSetup() {
        setOrientationListener();

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(@NonNull LocaleChangedEvent event) {
                recreate();
            }
        });
        disposables.wire(CamouflageAliasChangedEvent.class, new EventObserver<CamouflageAliasChangedEvent>() {
            @Override
            public void onNext(@NonNull CamouflageAliasChangedEvent event) {
                closeApp();
            }
        });
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_host);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();
        btmNavMain = findViewById(R.id.btm_nav_main);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeScreen, R.id.cameraScreen, R.id.reportsScreen, R.id.uwaziScreen, R.id.micScreen, R.id.formScreen).build();
        NavigationUI.setupWithNavController(btmNavMain, navController);
        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            switch (navDestination.getId()) {
                case (R.id.micScreen):
                    checkLocationSettings(C.START_AUDIO_RECORD, () -> {
                    });
                case (R.id.homeScreen):
                case R.id.formScreen:
                case R.id.uwaziScreen:
                    showBottomNavigation();
                    break;
                default:
                    hideBottomNavigation();
                    break;
            }
        });
    }

    private boolean isLocationSettingsRequestCode(int requestCode) {
        return requestCode == C.START_CAMERA_CAPTURE ||
                requestCode == C.START_AUDIO_RECORD;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == C.IMPORT_VIDEO) {
            if (data != null) {
                Uri video = data.getData();
                if (video != null) {
                    mediaImportPresenter.importVideo(video);
                }
            }
            return;
        }

        if (requestCode == C.IMPORT_IMAGE) {
            if (data != null) {
                Uri image = data.getData();
                if (image != null) {
                    mediaImportPresenter.importImage(image);
                }
            }
            return;
        }

        if (requestCode == C.IMPORT_FILE) {
            if (data != null) {
                Uri file = data.getData();
                if (file != null) {
                    mediaImportPresenter.importFile(file);
                }
            }
            return;
        }

        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            return; // user canceled evidence acquiring
        }

        List<Fragment> fragments = Objects.requireNonNull(getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
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
        if (checkCurrentFragment()) return;
        if (!checkIfShouldExit()) return;
        closeApp();
    }

    private boolean checkCurrentFragment() {
        List<Fragment> fragments = Objects.requireNonNull(getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof AttachmentsFragment) {
                ((AttachmentsFragment) fragment).onBackPressed();
                return true;
            }

            if (fragment instanceof DownloadedTemplatesFragment ||
                    fragment instanceof UwaziEntryFragment ||
                    fragment instanceof SubmittedPreviewFragment ||
                    fragment instanceof UwaziSendFragment) {
                navController.popBackStack();
                return true;
            }
        }
        return false;
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
        btmNavMain.getMenu().findItem(R.id.home).setChecked(true);
        homeScreenPresenter.countCollectServers();
        homeScreenPresenter.countUwaziServers();
        startLocationMetadataListening();
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationMetadataListening();

        mOrientationEventListener.disable();
    }

    @Override
    protected void onStop() {
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
    public void onMediaFileImported(VaultFile vaultFile) {
        List<String> list = new ArrayList<>();
        list.add(vaultFile.id);
        onActivityResult(C.MEDIA_FILE_ID, RESULT_OK, new Intent().putExtra(VAULT_FILE_KEY, new Gson().toJson(list)));
    }

    @Override
    public void onImportError(Throwable error) {

    }

    @Override
    public void onImportStarted() {

    }

    @Override
    public void onImportEnded() {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCountTUServersEnded(Long num) {
        if (num > 0)
            CleanInsightUtils.INSTANCE.measureEvent(CleanInsightUtils.ServerType.SERVER_TELLA);
    }

    @Override
    public void onCountTUServersFailed(Throwable throwable) {
        Timber.d(throwable);
    }

    @Override
    public void onCountCollectServersEnded(Long num) {
        maybeShowFormsMenu(num);
        if (num > 0)
            CleanInsightUtils.INSTANCE.measureEvent(CleanInsightUtils.ServerType.SERVER_COLLECT);
        //homeScreenPresenter.countTUServers();
    }

    @Override
    public void onCountCollectServersFailed(Throwable throwable) {

    }

    @Override
    public void onCountUwaziServersEnded(Long num) {
        maybeShowUwaziMenu(num);
        if (num > 0)
            CleanInsightUtils.INSTANCE.measureEvent(CleanInsightUtils.ServerType.SERVER_UWAZI);
    }

    @Override
    public void onCountUwaziServersFailed(Throwable throwable) {

    }

    private void stopPresenter() {
        if (homeScreenPresenter != null) {
            homeScreenPresenter.destroy();
            homeScreenPresenter = null;
        }

        if (mediaImportPresenter != null) {
            mediaImportPresenter.destroy();
            mediaImportPresenter = null;
        }
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
                //if (!isInCameraMode) return;
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
                // handle rotation for tablets;
            }
        };
    }

    public void hideBottomNavigation() {
        btmNavMain.setVisibility(View.GONE);
    }

    public void showBottomNavigation() {
        btmNavMain.setVisibility(View.VISIBLE);
    }

    public void selectNavMic() {
        btmNavMain.getMenu().findItem(R.id.mic).setChecked(true);
    }

    public void selectNavForm() {
        btmNavMain.getMenu().findItem(R.id.form).setChecked(true);
    }

    private void maybeShowFormsMenu(Long num) {
        btmNavMain.getMenu().findItem(R.id.form).setVisible(num > 0);
        invalidateOptionsMenu();
    }

    private void maybeShowUwaziMenu(Long num) {
        btmNavMain.getMenu().findItem(R.id.uwazi).setVisible(num > 0);
        invalidateOptionsMenu();
    }
}

