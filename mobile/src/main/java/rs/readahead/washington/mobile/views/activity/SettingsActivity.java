package rs.readahead.washington.mobile.views.activity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_CAMOUFLAGE;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

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
public class SettingsActivity extends BaseLockActivity {
    private final CamouflageManager cm = CamouflageManager.getInstance();
    private ActivitySettingsBinding binding;
    private EventCompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);

        if (fragment instanceof SendFeedbackFragment) {
            ((SendFeedbackFragment) fragment).handleBackButton();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // If there are fragments on the back stack, pop the top fragment
            getSupportFragmentManager().popBackStack();
        } else {
            // If there are no fragments on the back stack, let the system handle the back button
            super.onBackPressed();
        }
    }
}
