package org.horizontal.tella.mobile.views.activity;

import static com.hzontal.tella_vault.Metadata.VIEW_METADATA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.databinding.ActivityMetadataViewerBinding;
import org.horizontal.tella.mobile.views.activity.viewer.SharedMediaFileViewModel;
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategory;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationCategoryBinder;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationField;
import org.horizontal.tella.mobile.views.activity.viewer.VerificationMetadataRows;
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity;
import org.hzontal.shared_ui.utils.DialogUtils;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class MetadataViewerActivity extends BaseLockActivity {

    public static final String PARENT_ID = "metadata_parent_id";
    public static final String CATEGORY = "verification_category";

    LinearLayout metadataList;
    private VaultFile vaultFile;
    private Metadata metadata;
    private String parentId;
    private SharedMediaFileViewModel viewModel;
    private ActivityMetadataViewerBinding binding;
    private boolean openedDirectlyToCategory;
    private boolean showingDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMetadataViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdgeDarkBackground(binding.getRoot());

        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);

        metadataList = binding.content.metadataList;
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.verification_info_app_bar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.verification_info_app_bar);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.setOutlineProvider(null);
        } else {
            binding.appbar.bringToFront();
        }

        if (getIntent().hasExtra(VIEW_METADATA)) {
            VaultFile extraFile = (VaultFile) getIntent().getExtras().get(VIEW_METADATA);
            if (extraFile != null) {
                this.vaultFile = extraFile;
            }
        }
        if (getIntent().hasExtra(PARENT_ID)) {
            parentId = getIntent().getStringExtra(PARENT_ID);
        }

        metadata = vaultFile != null ? vaultFile.metadata : null;
        VerificationCategory initialCategory = getCategoryExtra();
        openedDirectlyToCategory = initialCategory != null;

        viewModel = new ViewModelProvider(this).get(SharedMediaFileViewModel.class);
        viewModel.getVerificationMetadataSaved().observe(this, file ->
                DialogUtils.showBottomMessage(
                        this,
                        getString(R.string.verification_save_csv_vault_success, file.name),
                        false
                )
        );
        viewModel.getVerificationMetadataAlreadySaved().observe(this, path ->
                VaultActionsHelper.showVerificationCsvAlreadySavedSheet(
                        getSupportFragmentManager(),
                        this,
                        path
                )
        );
        viewModel.getError().observe(this, resId -> {
            if (resId != null) {
                DialogUtils.showBottomMessage(this, getString(resId), true);
            }
        });

        binding.content.saveCsvButton.setOnClickListener(v -> {
            if (vaultFile != null) {
                viewModel.saveVerificationMetadata(vaultFile, parentId);
            }
        });

        VerificationCategoryBinder.bind(
                binding.content.verificationCategories.getRoot(),
                this::showCategoryDetail
        );

        if (initialCategory != null) {
            showCategoryDetail(initialCategory);
        } else {
            showCategoryList();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    public void onBackPressed() {
        if (showingDetail && !openedDirectlyToCategory) {
            showCategoryList();
            return;
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.metadata_viewer_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.help_item) {
            startMetadataHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCategoryList() {
        showingDetail = false;
        setScreenTitle(R.string.verification_info_app_bar);
        binding.content.verificationCategories.getRoot().setVisibility(View.VISIBLE);
        metadataList.setVisibility(View.GONE);
        metadataList.removeAllViews();
        binding.content.saveCsvButton.setVisibility(
                metadata != null ? View.VISIBLE : View.GONE
        );
    }

    private void showCategoryDetail(VerificationCategory category) {
        if (vaultFile == null) {
            return;
        }
        showingDetail = true;
        setScreenTitle(titleFor(category));
        binding.content.verificationCategories.getRoot().setVisibility(View.GONE);
        metadataList.setVisibility(View.VISIBLE);
        metadataList.removeAllViews();
        binding.content.saveCsvButton.setVisibility(View.GONE);

        for (VerificationField field : VerificationMetadataRows.INSTANCE.rows(vaultFile, category)) {
            metadataList.addView(createMetadataItem(field.getValue(), getString(field.getLabelRes())));
        }
    }

    private void setScreenTitle(@StringRes int titleResId) {
        binding.toolbar.setTitle(titleResId);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleResId);
        }
    }

    @StringRes
    private int titleFor(VerificationCategory category) {
        switch (category) {
            case FILE:
                return R.string.verification_info_subheading_file_metadata;
            case DEVICE:
                return R.string.verification_info_subheading_device_metadata;
            case NETWORK:
                return R.string.verification_info_subheading_network_metadata;
            case LOCATION:
                return R.string.verification_info_subheading_location_metadata;
            case OTHER:
                return R.string.verification_info_subheading_other_metadata;
            default:
                return R.string.verification_info_app_bar;
        }
    }

    private View createMetadataItem(CharSequence value, String name) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.metadata_item, null);

        TextView dataName = layout.findViewById(R.id.name);
        TextView dataValue = layout.findViewById(R.id.data);

        dataName.setText(name);
        if (value == null || value.length() < 1) {
            dataValue.setText(R.string.verification_info_field_metadata_not_available);
        } else {
            dataValue.setText(value);
        }

        return layout;
    }

    @SuppressWarnings("deprecation")
    private VerificationCategory getCategoryExtra() {
        Object extra = getIntent().getSerializableExtra(CATEGORY);
        if (extra instanceof VerificationCategory) {
            return (VerificationCategory) extra;
        }
        return null;
    }

    private void startMetadataHelp() {
        startActivity(new Intent(MetadataViewerActivity.this, MetadataHelpActivity.class));
    }
}
