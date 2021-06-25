package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.hzontal.shared_ui.appbar.CollapsableAppBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class LanguageSettingsActivity extends BaseLockActivity implements
        View.OnClickListener {
    @BindView(R.id.language_list)
    LinearLayout LanguageList;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    /*@BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;*/
    @BindView(R.id.collapsable_appbar)
    CollapsableAppBar collapsableAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);

        ButterKnife.bind(this);

        setSupportActionBar(collapsableAppBar.toolbar);
     //   collapsingToolbarLayout.setTitle(getString(R.string.settings_gen_select_language));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        createLangViews(LocaleManager.getInstance().getLanguageSetting());
    }

    private void createLangViews(@Nullable String prefLanguage) {
        ArrayList<String> languages = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.ra_lang_codes)));
        languages.add(0, null);

        for (String language: languages) {
            View view = getLanguageItem(language, TextUtils.equals(prefLanguage, language));
            view.setOnClickListener(this);
            LanguageList.addView(view);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setAppLanguage(@Nullable String language) {
        Locale locale = language != null ? new Locale(language) : null;
        LocaleManager.getInstance().setLocale(locale);

        MyApplication.bus().post(new LocaleChangedEvent(locale));
        recreate();
    }

    @Override
    public void onClick(View v) {
        setAppLanguage((String) v.getTag());
    }

    private View getLanguageItem(@Nullable final String language, boolean selected) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        FrameLayout item = (FrameLayout) inflater.inflate(R.layout.language_item, null);
        TextView langName = item.findViewById(R.id.lang);
        TextView langInfo = item.findViewById(R.id.lang_info);
        ImageView imageView = item.findViewById(R.id.language_check);

        item.setTag(language);

        if (language == null) {
            langName.setText(R.string.settings_lang_select_default);
            langInfo.setText(R.string.settings_lang_select_expl_default);
        } else {
            Locale locale = new Locale(language);
            langName.setText(StringUtils.capitalize(locale.getDisplayName(), locale));
            langInfo.setText(StringUtils.capitalize(locale.getDisplayName(locale), locale));
        }

        imageView.setVisibility(selected ? View.VISIBLE : View.GONE);

        return item;
    }
}
