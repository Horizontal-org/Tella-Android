package org.horizontal.tella.mobile.views.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.databinding.ActivityMetadataHelpBinding;
import org.horizontal.tella.mobile.util.Util;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategory;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategoryBinder;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationHelpField;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationHelpRows;
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity;

public class MetadataHelpActivity extends BaseLockActivity {
    private ActivityMetadataHelpBinding binding;
    private boolean showingCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);

        binding = ActivityMetadataHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdgeDarkBackground(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.setOutlineProvider(null);
        } else {
            binding.appbar.bringToFront();
        }

        bindIntro();
        VerificationCategoryBinder.bind(
                binding.content.verificationCategories.getRoot(),
                this::showCategoryHelp
        );
        showOverview();
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
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    public void onBackPressed() {
        if (showingCategory) {
            showOverview();
            return;
        }
        finish();
    }

    private void bindIntro() {
        String learnMore = getString(R.string.action_learn_more);
        String intro = getString(R.string.verification_help_intro, learnMore);
        SpannableString spannable = new SpannableString(intro);
        int start = intro.lastIndexOf(learnMore);
        if (start >= 0) {
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Util.startBrowserIntent(
                            MetadataHelpActivity.this,
                            getString(R.string.config_verification_url)
                    );
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(ContextCompat.getColor(MetadataHelpActivity.this, R.color.wa_orange));
                    ds.setUnderlineText(false);
                }
            }, start, start + learnMore.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.content.helpIntroTv.setText(spannable);
        binding.content.helpIntroTv.setMovementMethod(LinkMovementMethod.getInstance());
        binding.content.helpIntroTv.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void showOverview() {
        showingCategory = false;
        setScreenTitle(R.string.verification_help_info_app_bar);
        binding.content.helpOverview.setVisibility(View.VISIBLE);
        binding.content.metadataHelpList.setVisibility(View.GONE);
        binding.content.metadataHelpList.removeAllViews();
    }

    private void showCategoryHelp(VerificationCategory category) {
        showingCategory = true;
        setScreenTitle(VerificationHelpRows.INSTANCE.titleRes(category));
        binding.content.helpOverview.setVisibility(View.GONE);
        binding.content.metadataHelpList.setVisibility(View.VISIBLE);
        binding.content.metadataHelpList.removeAllViews();

        for (VerificationHelpField field : VerificationHelpRows.INSTANCE.rows(category)) {
            binding.content.metadataHelpList.addView(
                    createHelpItem(getString(field.getLabelRes()), getString(field.getExplanationRes()))
            );
        }
    }

    private void setScreenTitle(@StringRes int titleResId) {
        binding.toolbar.setTitle(titleResId);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleResId);
        }
    }

    private View createHelpItem(String name, String explanation) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.metadata_item, null);

        TextView dataName = layout.findViewById(R.id.name);
        TextView dataValue = layout.findViewById(R.id.data);
        dataName.setText(name);
        dataValue.setText(explanation);
        return layout;
    }
}
