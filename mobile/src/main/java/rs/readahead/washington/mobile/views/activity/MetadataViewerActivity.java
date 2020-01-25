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


public class MetadataViewerActivity extends CacheWordSubscriberBaseActivity {
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
            //noinspection ConstantConditions
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

    private View createMetadataItem(CharSequence value, String name) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.metadata_item, null);

        TextView dataName = layout.findViewById(R.id.name);
        TextView dataValue = layout.findViewById(R.id.data);

        dataName.setText(name);
        if (value == null || value.length() < 1) {
            dataValue.setText(R.string.not_available);
        } else {
            dataValue.setText(value);
        }

        return layout;
    }

    private void showMetadata() {
        if (mediaFile == null || metadata == null) {
            return;
        }

        metadataList.addView(createMetadataTitle(R.string.file));
        metadataList.addView(createMetadataItem(metadata.getFileName() != null ?
                metadata.getFileName() : mediaFile.getFileName(), getResources().getString(R.string.filename)));
        metadataList.addView(createMetadataItem(mediaFile.getPath(), getResources().getString(R.string.file_path)));

        metadataList.addView(createMetadataItem(mediaFile.getHash() != null ?
                mediaFile.getHash() : metadata.getFileHashSHA256(), getResources().getString(R.string.filehash)));
        metadataList.addView(createMetadataItem(
                Util.getDateTimeString(mediaFile.getCreated(), "dd-MM-yyyy HH:mm:ss Z"), getResources().getString(R.string.file_modified)));

        metadataList.addView(createMetadataTitle(R.string.device));
        metadataList.addView(createMetadataItem(metadata.getManufacturer(), getResources().getString(R.string.manufacturer)));
        metadataList.addView(createMetadataItem(metadata.getHardware(), getResources().getString(R.string.hardware)));
        metadataList.addView(createMetadataItem(metadata.getDeviceID(), getResources().getString(R.string.device_id)));
        metadataList.addView(createMetadataItem(metadata.getScreenSize(), getResources().getString(R.string.screen_size)));
        metadataList.addView(createMetadataItem(metadata.getLanguage(), getResources().getString(R.string.language)));
        metadataList.addView(createMetadataItem(metadata.getLocale(), getResources().getString(R.string.locale)));
        metadataList.addView(createMetadataItem(metadata.getNetwork(), getResources().getString(R.string.connection_status)));
        metadataList.addView(createMetadataItem(metadata.getNetworkType(), getResources().getString(R.string.network_type)));
        metadataList.addView(createMetadataItem(metadata.getWifiMac(), getResources().getString(R.string.wifi_mac)));
        metadataList.addView(createMetadataItem(metadata.getIPv4(), getResources().getString(R.string.ipv4)));
        metadataList.addView(createMetadataItem(metadata.getIPv6(), getResources().getString(R.string.ipv6)));

        metadataList.addView(createMetadataTitle(R.string.context));

        if (metadata.getMyLocation() != null) {
            metadataList.addView(createMetadataItem(getLocationString(metadata.getMyLocation()), getResources().getString(R.string.location)));
            metadataList.addView(createMetadataItem(metadata.getMyLocation().getProvider(), getResources().getString(R.string.location_provider)));
            metadataList.addView(createMetadataItem(metadata.getMyLocation().getSpeed().toString(), getResources().getString(R.string.location_speed)));
        } else {
            metadataList.addView(createMetadataItem(getString(R.string.not_available), getResources().getString(R.string.location)));
        }

        String cells = StringUtils.join(", ", metadata.getCells());
        metadataList.addView(createMetadataItem(cells, getResources().getString(R.string.cell_info)));

        metadataList.addView(createMetadataItem(
                metadata.getWifis() != null ? TextUtils.join(", ", metadata.getWifis()) : getString(R.string.not_available),
                getString(R.string.ra_wifi_info)));
    }

    private String getLocationString(MyLocation myLocation) {
        return getString(R.string.latitude) + ": " + myLocation.getLatitude() + '\n' +
                getString(R.string.longitude) + ": " + myLocation.getLongitude() + '\n' +
                getString(R.string.altitude) + ": " + myLocation.getAltitude() + '\n' +
                getString(R.string.accuracy) + ": " + myLocation.getAccuracy().toString() + '\n' +
                getString(R.string.time) + ": " + Util.getDateTimeString(myLocation.getTimestamp(), "dd-MM-yyyy HH:mm:ss Z");
    }

    private void startMetadataHelp() {
        startActivity(new Intent(MetadataViewerActivity.this, MetadataHelpActivity.class));
    }
}
