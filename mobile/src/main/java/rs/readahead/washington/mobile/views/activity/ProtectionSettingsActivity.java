package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.CamouflageManager;


public class ProtectionSettingsActivity extends CacheWordSubscriberBaseActivity implements CompoundButton.OnCheckedChangeListener {
    //@BindView(R.id.show_camera_preview)
    //SwitchCompat cameraPreviewSwitch;
    @BindView(R.id.camouflage_setting)
    TextView camouflageSetting;
    @BindView(R.id.quick_exit_switch)
    SwitchCompat quickExitSwitch;
    @BindView(R.id.quick_exit_settings_layout)
    View quickExitSettings;
    @BindView(R.id.delete_gallery)
    CheckBox deleteGallery;
    @BindView(R.id.delete_forms)
    CheckBox deleteForms;
    @BindView(R.id.delete_server_settings)
    CheckBox deleteServerSettings;
    @BindView(R.id.delete_tella)
    CheckBox deleteTella;

    private static final String defaultAlias = SplashActivity.class.getCanonicalName();
    private CamouflageManager cm = CamouflageManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protection_settings);

        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //setupCameraPreviewSwitch();
        setupQuickExitSwitch();
        setupQuickExitSettingsView();
        setupQuickExitCheckboxes();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.protection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setCamouflageOption();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.camouflage_settings)
    public void manage(View view) {
        startActivity(new Intent(this, CamouflageAliasActivity.class));
    }
/*
    private void setupCameraPreviewSwitch() {
        cameraPreviewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setCameraPreviewEnabled(isChecked));
        cameraPreviewSwitch.setChecked(Preferences.isCameraPreviewEnabled());
    }
*/
    private void setupQuickExitSwitch() {
        quickExitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setQuickExit(isChecked);
            setupQuickExitSettingsView();
        });
    }

    private void setupQuickExitSettingsView() {
        if (Preferences.isQuickExit()) {
            quickExitSwitch.setChecked(true);
            quickExitSettings.setVisibility(View.VISIBLE);
        } else {
            quickExitSwitch.setChecked(false);
            quickExitSettings.setVisibility(View.GONE);
        }
    }

    private void setupQuickExitCheckboxes() {
        deleteTella.setOnCheckedChangeListener(this);
        deleteTella.setChecked(Preferences.isUninstallOnPanic());

        deleteForms.setOnCheckedChangeListener(this);
        deleteForms.setChecked(Preferences.isEraseForms());

        deleteGallery.setOnCheckedChangeListener(this);
        deleteGallery.setChecked(Preferences.isDeleteGalleryEnabled());

        deleteServerSettings.setOnCheckedChangeListener(this);
        deleteServerSettings.setChecked(Preferences.isDeleteServerSettingsActive());
    }

    private void setCamouflageOption() {
        if (cm.isDefaultLauncherActivityAlias()) {
            camouflageSetting.setText(R.string.ra_default);
        } else {
            camouflageSetting.setText(cm.getLauncherName(this));
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.delete_forms:
                deleteFormsCheck(isChecked);
                break;
            case R.id.delete_gallery:
                deleteGalleryCheck(isChecked);
                break;
            case R.id.delete_server_settings:
                deleteServerSettingsCheck(isChecked);
                break;
            case R.id.delete_tella:
                deleteTellaCheck(isChecked);
                break;
        }
    }

    private void deleteTellaCheck(boolean isChecked) {
        if (isChecked) {
            deleteTella.setChecked(true);
            Preferences.setUninstallOnPanic(true);
            deleteGallery.setChecked(true);
            deleteGallery.setClickable(false);
            deleteGallery.setAlpha((float) 0.5);
            deleteServerSettingsCheck(true);
            deleteServerSettings.setClickable(false);
            deleteServerSettings.setAlpha((float) 0.5);
        } else {
            deleteTella.setChecked(false);
            Preferences.setUninstallOnPanic(false);
            deleteGallery.setChecked(false);
            deleteGallery.setClickable(true);
            deleteGallery.setAlpha((float) 1);
            deleteServerSettingsCheck(false);
            deleteServerSettings.setClickable(true);
            deleteServerSettings.setAlpha((float) 1);
        }
    }

    private void deleteFormsCheck(boolean isChecked) {
        deleteForms.setChecked(isChecked);
        Preferences.setEraseForms(isChecked);
    }

    private void deleteGalleryCheck(boolean isChecked) {
        deleteGallery.setChecked(isChecked);
        Preferences.setDeleteGallery(isChecked);
    }

    private void deleteServerSettingsCheck(boolean isChecked) {
        if (isChecked) {
            deleteServerSettings.setChecked(true);
            Preferences.setDeleteServerSettingsActive(true);
            deleteFormsCheck(true);
            deleteForms.setClickable(false);
            deleteForms.setAlpha((float) 0.5);
        } else {
            deleteServerSettings.setChecked(false);
            Preferences.setDeleteServerSettingsActive(false);
            deleteFormsCheck(false);
            deleteForms.setClickable(true);
            deleteForms.setAlpha((float) 1);
        }
    }
}
