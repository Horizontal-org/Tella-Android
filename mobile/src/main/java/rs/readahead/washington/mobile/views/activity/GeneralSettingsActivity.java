package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.StringUtils;


public class GeneralSettingsActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.crash_report_switch)
    SwitchCompat crashReportSwitch;
    @BindView(R.id.lang)
    TextView langSetting;

    private EventCompositeDisposable disposables;
    private CompositeDisposable disposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ra_general);
        }

        disposable = new CompositeDisposable();

        setLanguage();
        setupCrashReportsSwitch();

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(LocaleChangedEvent event) {
                recreate();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.language_settings)
    public void startActivity() {
        startActivity(new Intent(this, LanguageSettingsActivity.class));
    }


    @Override
    public void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        if (disposable != null) {
            disposable.dispose();
        }

        super.onDestroy();
    }

    private void setupCrashReportsSwitch() {
        crashReportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setSubmittingCrashReports(isChecked));
        crashReportSwitch.setChecked(Preferences.isSubmittingCrashReports());
    }

    private void setLanguage() {
        String languageSetting = LocaleManager.getInstance().getLanguageSetting();
        if (languageSetting == null) {
            langSetting.setText(R.string.ra_default);
        } else {
            Locale locale = new Locale(languageSetting);
            langSetting.setText(StringUtils.capitalize(locale.getDisplayName(locale), locale));
        }
    }
}
