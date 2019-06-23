package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;


public class SettingsActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private EventCompositeDisposable disposables;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(LocaleChangedEvent.class, new EventObserver<LocaleChangedEvent>() {
            @Override
            public void onNext(LocaleChangedEvent event) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.security_settings_layout, R.id.collect_settings, R.id.general_settings, R.id.about_n_help_layout})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.general_settings:
                startActivity(new Intent(this, GeneralSettingsActivity.class));
                break;
            case R.id.security_settings_layout:
                startActivity(new Intent(this, ProtectionSettingsActivity.class));
                break;
            case R.id.collect_settings:
                startActivity(new Intent(this, DocumentationSettingsActivity.class));
                break;
            case R.id.about_n_help_layout:
                startActivity(new Intent(this, AboutHelpActivity.class));
                break;
        }
    }
}
