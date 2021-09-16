package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.os.Build;
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
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class MetadataHelpActivity extends BaseLockActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.metadata_help_list)
    LinearLayout metadataList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata_help);
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.verification_help_info_app_bar);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
    }

    @Override
    public void onBackPressed() {
        finish();
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
            dataValue.setText(R.string.verification_info_field_metadata_not_available);
        } else {
            dataValue.setText(value);
        }

        return layout;
    }

    private void showMetadataHelp() {

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_file_metadata));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_file_path_expl), getResources().getString(R.string.verification_info_field_file_path)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_hash_expl), getResources().getString(R.string.verification_info_field_hash)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_date_time_modified_expl), getResources().getString(R.string.verification_info_field_file_modified)));

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_device_metadata));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_manufacturer_expl), getResources().getString(R.string.verification_info_field_manufacturer)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_device_model_expl), getResources().getString(R.string.verification_info_field_hardware)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_device_id_expl), getResources().getString(R.string.verification_info_field_device_id)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_screen_size_expl), getResources().getString(R.string.verification_info_field_screen_size)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_language_expl), getResources().getString(R.string.verification_info_field_language)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_locale_expl), getResources().getString(R.string.verification_info_field_locale)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_connection_status_expl), getResources().getString(R.string.verification_info_field_connection_status)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_network_type_expl), getResources().getString(R.string.verification_info_field_network_type)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_wifi_mac_expl), getResources().getString(R.string.verification_info_field_wifi_mac)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_ipv4_expl), getResources().getString(R.string.verification_info_field_ipv4)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_ipv6_expl), getResources().getString(R.string.verification_info_field_ipv6)));

        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_context_metadata));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_location_expl), getResources().getString(R.string.verification_info_field_location)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_location_provider_expl), getResources().getString(R.string.verification_info_field_location_provider)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_location_speed_expl), getResources().getString(R.string.verification_info_field_location_speed)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_cell_towers_expl), getResources().getString(R.string.verification_info_field_cell_towers)));
        metadataList.addView(createMetadataItem(getResources().getString(R.string.verification_info_wifi_expl), getResources().getString(R.string.verification_info_wifi)));
    }
}
