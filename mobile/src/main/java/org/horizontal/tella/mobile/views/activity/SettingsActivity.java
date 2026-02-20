package org.horizontal.tella.mobile.views.activity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_CAMOUFLAGE;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import org.hzontal.shared_ui.appbar.ToolbarComponent;
import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.bus.EventCompositeDisposable;
import org.horizontal.tella.mobile.bus.EventObserver;
import org.horizontal.tella.mobile.bus.event.CloseSettingsActivityEvent;
import org.horizontal.tella.mobile.bus.event.GoToReportsScreenEvent;
import org.horizontal.tella.mobile.bus.event.LocaleChangedEvent;
import org.horizontal.tella.mobile.databinding.ActivitySettingsBinding;
import org.horizontal.tella.mobile.util.CamouflageManager;
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity;
import org.horizontal.tella.mobile.views.fragment.feedback.SendFeedbackFragment;
import org.horizontal.tella.mobile.views.settings.ChangeRemoveCamouflage;
import org.horizontal.tella.mobile.views.settings.HideTella;
import org.horizontal.tella.mobile.views.settings.MainSettings;
import org.horizontal.tella.mobile.views.settings.OnFragmentSelected;
import org.horizontal.tella.mobile.views.settings.SecuritySettings;

@AndroidEntryPoint
public class SettingsActivity extends BaseLockActivity implements OnFragmentSelected {
    private final CamouflageManager cm = CamouflageManager.getInstance();
    protected boolean isCamouflage = false;
    private ActivitySettingsBinding binding;
    private EventCompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setBackClickListener(() -> {
            onBackPressed();
            return null;
        });

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setStartTextTitle(getResources().getString(R.string.settings_app_bar));
        setSupportActionBar(binding.toolbar);

        findViewById(R.id.appbar).setOutlineProvider(null);

        if (getIntent().hasExtra(IS_CAMOUFLAGE)) {
            addFragment(new MainSettings(), R.id.my_nav_host_fragment);
            addFragment(new SecuritySettings(), R.id.my_nav_host_fragment);
            if (cm.isDefaultLauncherActivityAlias()) {
                addFragment(new HideTella(), R.id.my_nav_host_fragment);
            } else {
                addFragment(new ChangeRemoveCamouflage(), R.id.my_nav_host_fragment);
            }
        }

        if (getIntent().hasExtra(IS_CAMOUFLAGE)) {
            addFragment(new MainSettings(),R.id.my_nav_host_fragment);
            addFragment(new SecuritySettings(),R.id.my_nav_host_fragment);
            if (cm.isDefaultLauncherActivityAlias()) {
                addFragment(new HideTella(),R.id.my_nav_host_fragment);
            } else {
                addFragment(new ChangeRemoveCamouflage(),R.id.my_nav_host_fragment);
            }
        }

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<>() {
            @Override
            public void onNext(@NotNull LocaleChangedEvent event) {
                recreate();
            }
        });

        disposables.wire(CloseSettingsActivityEvent.class, new EventObserver<>() {
            @Override
            public void onNext(@NotNull CloseSettingsActivityEvent event) {
                finish();
                MyApplication.bus().post(new GoToReportsScreenEvent());
            }
        });

    }

    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        super.onDestroy();
    }

    @Override
    public void setToolbarLabel(int labelRes) {
        binding.toolbar.setStartTextTitle(getString(labelRes));
    }

    @Override
    public void hideAppbar() {
        binding.toolbar.setVisibility(View.GONE);
    }

    @Override
    public void showAppbar() {
        binding.toolbar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setToolbarHomeIcon(int iconRes) {
        binding.toolbar.setToolbarNavigationIcon(iconRes);
    }

    public ToolbarComponent getToolbar() {
        return binding.toolbar;
    }

    @Override
    public boolean isCamouflage() {
        return isCamouflage;
    }

    //TODO needs an improvement
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);

        if (f != null) {
            if (f instanceof MainSettings) {
                showAppbar();
                setToolbarLabel(R.string.settings_app_bar);
            } else if (f instanceof SecuritySettings) {
                showAppbar();
                setToolbarLabel(R.string.settings_sec_app_bar);
            } else if (f instanceof SendFeedbackFragment) {
                ((SendFeedbackFragment) f).handleBackButton();
            }
        }
    }
}
