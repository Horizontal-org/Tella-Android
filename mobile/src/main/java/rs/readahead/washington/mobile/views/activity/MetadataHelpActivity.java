package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;


public class MetadataHelpActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.metadata_help_list)
    LinearLayout metadataList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata_help);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.metadata_help);
        }

        showMetadataHelp();
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

    private void showMetadataHelp() {
        String colon = String.valueOf(':');

        metadataList.addView(createMetadataTitle(R.string.file));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.file_path_info), getResources().getString(R.string.file_path) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.file_hash_info), getResources().getString(R.string.filehash) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.file_modified_info), getResources().getString(R.string.file_modified) + colon));

        metadataList.addView(createMetadataTitle(R.string.device));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.manufacturer_info), getResources().getString(R.string.manufacturer) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.hardware_info), getResources().getString(R.string.hardware) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.device_id_info), getResources().getString(R.string.device_id) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.screen_size_info), getResources().getString(R.string.screen_size) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.language_info), getResources().getString(R.string.language) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.locale_info), getResources().getString(R.string.locale) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.connection_status_info), getResources().getString(R.string.connection_status) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.network_type_info), getResources().getString(R.string.network_type) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.wifi_mac_info), getResources().getString(R.string.wifi_mac) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.ipv4_info), getResources().getString(R.string.ipv4) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.ipv6_info), getResources().getString(R.string.ipv6) + colon));

        metadataList.addView(createMetadataTitle(R.string.context));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.location_information), getResources().getString(R.string.location) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.location_provider_info), getResources().getString(R.string.location_provider) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.location_speed_info), getResources().getString(R.string.location_speed) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.cell_info_info), getResources().getString(R.string.cell_info) + colon));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.wifi_info_info), getResources().getString(R.string.ra_wifi_info) + colon));
    }
}
