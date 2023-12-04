package rs.readahead.washington.mobile.views.activity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_CAMOUFLAGE;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.hzontal.shared_ui.appbar.ToolbarComponent;
import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CloseSettingsActivityEvent;
import rs.readahead.washington.mobile.bus.event.GoToReportsScreenEvent;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.databinding.ActivitySettingsBinding;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.feedback.SendFeedbackFragment;
import rs.readahead.washington.mobile.views.settings.ChangeRemoveCamouflage;
import rs.readahead.washington.mobile.views.settings.HideTella;
import rs.readahead.washington.mobile.views.settings.MainSettings;
import rs.readahead.washington.mobile.views.settings.OnFragmentSelected;
import rs.readahead.washington.mobile.views.settings.SecuritySettings;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
        }

        if (getIntent().hasExtra(IS_CAMOUFLAGE)) {
            addFragment(new MainSettings(), R.id.my_nav_host_fragment);
            addFragment(new SecuritySettings(), R.id.my_nav_host_fragment);
            if (cm.isDefaultLauncherActivityAlias()) {
                addFragment(new HideTella(), R.id.my_nav_host_fragment);
            } else {
                addFragment(new ChangeRemoveCamouflage(), R.id.my_nav_host_fragment);
            }
        }

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(@NotNull LocaleChangedEvent event) {
                recreate();
            }
        });

        disposables.wire(CloseSettingsActivityEvent.class, new EventObserver<CloseSettingsActivityEvent>() {
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

    @Override
    public void onBackPressed() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);

        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();

            // Now 'currentFragment' represents the currently displayed fragment within the NavHostFragment.
            if (currentFragment != null) {
                // Do something with the current fragment
                if ((currentFragment instanceof SendFeedbackFragment)) {
                    ((SendFeedbackFragment) currentFragment).handleBackButton();
                } else {
                    super.onBackPressed();
                    if (currentFragment instanceof MainSettings) {
                        showAppbar();
                        setToolbarLabel(R.string.settings_app_bar);
                    } else if (currentFragment instanceof SecuritySettings) {
                        showAppbar();
                        setToolbarLabel(R.string.settings_sec_app_bar);
                    }
                }
            }
        }
    }
}
