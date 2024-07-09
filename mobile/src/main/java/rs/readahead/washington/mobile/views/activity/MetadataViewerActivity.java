package rs.readahead.washington.mobile.views.activity;

import static com.hzontal.tella_vault.Metadata.VIEW_METADATA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;

import rs.readahead.washington.mobile.R;

import rs.readahead.washington.mobile.databinding.ActivityMetadataViewerBinding;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class MetadataViewerActivity extends BaseLockActivity {

    LinearLayout metadataList;
    private VaultFile vaultFile;
    private Metadata metadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMetadataViewerBinding binding = ActivityMetadataViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);

        metadataList = binding.content.metadataList;
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

        if (getIntent().hasExtra(VIEW_METADATA)) {
            VaultFile vaultFile = (VaultFile) getIntent().getExtras().get(VIEW_METADATA);
            if (vaultFile != null) {
                this.vaultFile = vaultFile;
            }
        }

        metadata = vaultFile.metadata;

        showMetadata();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    public void onBackPressed() {
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

    private View createMetadataTitle(@StringRes int titleResId) {
        @SuppressLint("InflateParams")
        TextView textView = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.metadata_header, null);
        textView.setText(titleResId);
        return textView;
    }

    private LinearLayout createMetadataLine() {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.metadata_line, null);

        return layout;
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

    private void showMetadata() {
        if (vaultFile == null || metadata == null) {
            return;
        }

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_file_metadata));
        metadataList.addView(createMetadataItem(metadata.getFileName() != null ?
                metadata.getFileName() : vaultFile.name, getResources().getString(R.string.verification_info_field_filename)));
        metadataList.addView(createMetadataItem(vaultFile.path, getResources().getString(R.string.verification_info_field_file_path)));

        metadataList.addView(createMetadataItem(vaultFile.hash != null ?
                vaultFile.hash : metadata.getFileHashSHA256(), getResources().getString(R.string.verification_info_field_hash)));
        metadataList.addView(createMetadataItem(
                Util.getDateTimeString(vaultFile.created, "dd-MM-yyyy HH:mm:ss Z"), getResources().getString(R.string.verification_info_field_file_modified)));
        metadataList.addView(createMetadataLine());

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_device_metadata));
        metadataList.addView(createMetadataItem(metadata.getManufacturer(), getResources().getString(R.string.verification_info_field_manufacturer)));
        metadataList.addView(createMetadataItem(metadata.getHardware(), getResources().getString(R.string.verification_info_field_hardware)));
        metadataList.addView(createMetadataItem(metadata.getDeviceID(), getResources().getString(R.string.verification_info_field_device_id)));
        metadataList.addView(createMetadataItem(metadata.getScreenSize() + getResources().getString(R.string.inches),
                getResources().getString(R.string.verification_info_field_screen_size)));
        metadataList.addView(createMetadataItem(metadata.getLanguage(), getResources().getString(R.string.verification_info_field_language)));
        metadataList.addView(createMetadataItem(metadata.getLocale(), getResources().getString(R.string.verification_info_field_locale)));
        metadataList.addView(createMetadataItem(metadata.getNetwork(), getResources().getString(R.string.verification_info_field_connection_status)));
        metadataList.addView(createMetadataItem(metadata.getNetworkType(), getResources().getString(R.string.verification_info_field_network_type)));
        metadataList.addView(createMetadataItem(metadata.getWifiMac(), getResources().getString(R.string.verification_info_field_wifi_mac)));
        metadataList.addView(createMetadataItem(metadata.getIPv4(), getResources().getString(R.string.verification_info_field_ipv4)));
        metadataList.addView(createMetadataItem(metadata.getIPv6(), getResources().getString(R.string.verification_info_field_ipv6)));
        metadataList.addView(createMetadataLine());

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_context_metadata));

        if (metadata.getMyLocation() != null) {
            metadataList.addView(createMetadataItem(getLocationString(metadata.getMyLocation()), getResources().getString(R.string.verification_info_field_location)));
            metadataList.addView(createMetadataItem(metadata.getMyLocation().getProvider(), getResources().getString(R.string.verification_info_field_location_provider)));
            metadataList.addView(createMetadataItem(getString(R.string.Verification_Label_MeterPerSecond, metadata.getMyLocation().getSpeed()),
                    getResources().getString(R.string.verification_info_field_location_speed)));
        } else {
            metadataList.addView(createMetadataItem(getString(R.string.verification_info_field_metadata_not_available), getResources().getString(R.string.verification_info_field_location)));
        }

        if (metadata.getCells() != null) {
            String cells = StringUtils.join(", ", metadata.getCells());
            metadataList.addView(createMetadataItem(cells, getResources().getString(R.string.verification_info_field_cell_towers)));
        }

        metadataList.addView(createMetadataItem(
                metadata.getWifis() != null ? TextUtils.join(", ", metadata.getWifis()) : getString(R.string.verification_info_field_metadata_not_available),
                getString(R.string.verification_info_wifi)));
    }

    private String getLocationString(MyLocation myLocation) {
        return getString(R.string.verification_info_field_latitude) + myLocation.getLatitude() + '\n' +
                getString(R.string.verification_info_field_longitude) + myLocation.getLongitude() + '\n' +
                getString(R.string.verification_info_field_altitude) + getString(R.string.Verification_Label_meter, myLocation.getAltitude()) + '\n' +
                getString(R.string.verification_info_field_accuracy) + getString(R.string.Verification_Label_meter, myLocation.getAccuracy()) + '\n' +
                getString(R.string.verification_info_field_location_time) + Util.getDateTimeString(myLocation.getTimestamp(), "dd-MM-yyyy HH:mm:ss Z");
    }

    private void startMetadataHelp() {
        startActivity(new Intent(MetadataViewerActivity.this, MetadataHelpActivity.class));
    }
}
