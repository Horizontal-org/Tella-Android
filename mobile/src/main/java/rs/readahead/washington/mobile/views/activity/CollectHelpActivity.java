package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.StringUtils;


public class CollectHelpActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.servers_help)
    TextView mCollectServerTextView;
    @BindView(R.id.odk_help)
    TextView mODKTextView;
    @BindView(R.id.data_help)
    TextView mSaveDataTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_help);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_help);
        }

        mCollectServerTextView.setText(Html.fromHtml(getString(R.string.collect_help_server_info)));
        mCollectServerTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mCollectServerTextView);

        mODKTextView.setText(Html.fromHtml(getString(R.string.collect_help_odk)));
        mODKTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mODKTextView);

        mSaveDataTextView.setText(Html.fromHtml(getString(R.string.save_data)));
        mSaveDataTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mSaveDataTextView);
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
}
