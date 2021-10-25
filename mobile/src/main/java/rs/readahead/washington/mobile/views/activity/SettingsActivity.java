package rs.readahead.washington.mobile.views.activity;

import static com.hzontal.tella_locking_ui.ConstantsKt.IS_CAMOUFLAGE;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import android.view.View;

import androidx.fragment.app.Fragment;

import org.hzontal.shared_ui.appbar.ToolbarComponent;
import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.settings.ChangeRemoveCamouflage;
import rs.readahead.washington.mobile.views.settings.HideTella;
import rs.readahead.washington.mobile.views.settings.MainSettings;
import rs.readahead.washington.mobile.views.settings.OnFragmentSelected;
import rs.readahead.washington.mobile.views.settings.SecuritySettings;


public class SettingsActivity extends BaseLockActivity implements OnFragmentSelected {
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.toolbar)
    ToolbarComponent toolbar;
    
    private EventCompositeDisposable disposables;
    private final CamouflageManager cm = CamouflageManager.getInstance();
    protected boolean isCamouflage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        toolbar.setStartTextTitle(getResources().getString(R.string.settings_app_bar));
        setSupportActionBar(toolbar);

        toolbar.setBackClickListener(() -> {
            onBackPressed();
            return null;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
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
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(@NotNull LocaleChangedEvent event) {
                recreate();
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
        toolbar.setStartTextTitle(getString(labelRes));
    }

    @Override
    public void hideAppbar() {
        toolbar.setVisibility(View.GONE);
    }

    @Override
    public void showAppbar() {
        toolbar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setToolbarHomeIcon(int iconRes) {
        toolbar.setToolbarNavigationIcon(iconRes);
    }

    @Override
    public boolean isCamouflage() {
        return isCamouflage;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);

        if (f instanceof MainSettings) {
            showAppbar();
            setToolbarLabel(R.string.settings_app_bar);
        } else if (f instanceof SecuritySettings) {
            showAppbar();
            setToolbarLabel(R.string.settings_sec_app_bar);
        }
    }
}
