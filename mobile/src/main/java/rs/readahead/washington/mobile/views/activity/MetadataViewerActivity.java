package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.MyLocation;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class MetadataViewerActivity extends BaseLockActivity {
    public static final String VIEW_METADATA = "vm";

    @BindView(R.id.metadata_list)
    LinearLayout metadataList;

    private MediaFile mediaFile;
    private Metadata metadata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_metadata_viewer);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().hasExtra(VIEW_METADATA)) {
            MediaFile mediaFile = (MediaFile) getIntent().getExtras().get(VIEW_METADATA);
            if (mediaFile != null) {
                this.mediaFile = mediaFile;
            }
        }

        metadata = mediaFile.getMetadata();

        showMetadata();
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
        if (mediaFile == null || metadata == null) {
            return;
        }

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_file_metadata));
        metadataList.addView(createMetadataItem(metadata.getFileName() != null ?
                metadata.getFileName() : mediaFile.getFileName(), getResources().getString(R.string.verification_info_field_filename)));
        metadataList.addView(createMetadataItem(mediaFile.getPath(), getResources().getString(R.string.verification_info_field_file_path)));

        metadataList.addView(createMetadataItem(mediaFile.getHash() != null ?
                mediaFile.getHash() : metadata.getFileHashSHA256(), getResources().getString(R.string.verification_info_field_hash)));
        metadataList.addView(createMetadataItem(
                Util.getDateTimeString(mediaFile.getCreated(), "dd-MM-yyyy HH:mm:ss Z"), getResources().getString(R.string.verification_info_field_file_modified)));
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
            metadataList.addView(createMetadataItem(getString(R.string.meter_per_second, metadata.getMyLocation().getSpeed()),
                    getResources().getString(R.string.verification_info_field_location_speed)));
        } else {
            metadataList.addView(createMetadataItem(getString(R.string.verification_info_field_metadata_not_available), getResources().getString(R.string.verification_info_field_location)));
        }

        String cells = StringUtils.join(", ", metadata.getCells());
        metadataList.addView(createMetadataItem(cells, getResources().getString(R.string.verification_info_field_cell_towers)));

        metadataList.addView(createMetadataItem(
                metadata.getWifis() != null ? TextUtils.join(", ", metadata.getWifis()) : getString(R.string.verification_info_field_metadata_not_available),
                getString(R.string.verification_info_wifi)));
    }

    private String getLocationString(MyLocation myLocation) {
        return getString(R.string.verification_info_field_latitude) + myLocation.getLatitude() + '\n' +
                getString(R.string.verification_info_field_longitude) + myLocation.getLongitude() + '\n' +
                getString(R.string.verification_info_field_altitude) + getString(R.string.meter, myLocation.getAltitude()) + '\n' +
                getString(R.string.verification_info_field_accuracy) + getString(R.string.meter, myLocation.getAccuracy()) + '\n' +
                getString(R.string.verification_info_field_location_time) + Util.getDateTimeString(myLocation.getTimestamp(), "dd-MM-yyyy HH:mm:ss Z");
    }

    private void startMetadataHelp() {
        startActivity(new Intent(MetadataViewerActivity.this, MetadataHelpActivity.class));
    }
}
