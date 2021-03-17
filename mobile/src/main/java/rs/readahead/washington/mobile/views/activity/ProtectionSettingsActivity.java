package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity;
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity;
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity;

import org.hzontal.tella.keys.config.IUnlockRegistryHolder;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.mvp.contract.IProtectionSettingsPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.ProtectionSettingsPresenter;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_FROM_SETTINGS;


public class ProtectionSettingsActivity extends BaseLockActivity implements CompoundButton.OnCheckedChangeListener,
        IProtectionSettingsPresenterContract.IView {
    @BindView(R.id.camouflage_setting)
    TextView camouflageSetting;
    @BindView(R.id.quick_exit_switch)
    SwitchCompat quickExitSwitch;
    @BindView(R.id.quick_exit_settings_layout)
    View quickExitSettings;
    @BindView(R.id.delete_draft_submitted_forms_view)
    View deleteFormsView;
    @BindView(R.id.delete_server_settings_view)
    View deleteSettingsView;
    @BindView(R.id.delete_gallery)
    CheckBox deleteGallery;
    @BindView(R.id.delete_forms)
    CheckBox deleteForms;
    @BindView(R.id.delete_server_settings)
    CheckBox deleteServerSettings;
    @BindView(R.id.delete_tella)
    CheckBox deleteTella;
    @BindView(R.id.lock_setting)
    TextView lockSetting;

    private CamouflageManager cm = CamouflageManager.getInstance();
    private long numOfCollectServers = 0;
    private ProtectionSettingsPresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protection_settings);

        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        presenter = new ProtectionSettingsPresenter(this);

        setupQuickExitSwitch();
        setupQuickExitSettingsView();
        setupQuickExitCheckboxes();
        setUpLockTypeText();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_prot_app_bar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        countCollectServers();
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

    @Override
    protected void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @OnClick(R.id.camouflage_settings)
    public void manage(View view) {
        startActivity(new Intent(this, CamouflageAliasActivity.class));
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

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCountCollectServersEnded(long num) {
        numOfCollectServers = num;
        if (quickExitSettings.getVisibility() == View.VISIBLE && num > 0) {
            deleteFormsView.setVisibility(View.VISIBLE);
            deleteSettingsView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCountCollectServersFailed(Throwable throwable) {
    }

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
            if (numOfCollectServers == 0) {
                deleteFormsView.setVisibility(View.GONE);
                deleteSettingsView.setVisibility(View.GONE);
            }
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
            camouflageSetting.setText(R.string.settings_lang_select_default);
        } else {
            camouflageSetting.setText(cm.getLauncherName(this));
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

    private void countCollectServers() {
        if (presenter != null) {
            presenter.countCollectServers();
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void setUpLockTypeText(){
        switch (((IUnlockRegistryHolder) getApplicationContext()).getUnlockRegistry().getActiveMethod(this)){
            case TELLA_PIN:
                lockSetting.setText(getString(R.string.onboard_pin));
                break;
            case TELLA_PASSWORD:
                lockSetting.setText(getString(R.string.onboard_password));
                break;
            case TELLA_PATTERN:
                lockSetting.setText(getString(R.string.onboard_pattern));
                break;
        }
    }

    @OnClick(R.id.lock_settings)
    public void goToUnlockingActivity(){
        Intent intent = null;
        switch (((IUnlockRegistryHolder) getApplicationContext()).getUnlockRegistry().getActiveMethod(this)){
            case TELLA_PIN:
                intent =  new Intent(this,PinUnlockActivity.class);
                break;
            case TELLA_PASSWORD:
                intent =  new Intent(this, PasswordUnlockActivity.class);
                break;
            case TELLA_PATTERN:
                intent =  new Intent(this, PatternUnlockActivity.class);
                break;
        }
        intent.putExtra(IS_FROM_SETTINGS,true);
        startActivity(intent);
    }
}
