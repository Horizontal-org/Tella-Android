package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.ActivityCollectHelpBinding;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class CollectHelpActivity extends BaseLockActivity {
    Toolbar toolbar;
    TextView mCollectServerTextView;
    TextView mODKTextView;
    private ActivityCollectHelpBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCollectHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setViews();

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_help_app_bar);
        }

        mCollectServerTextView.setText(Html.fromHtml(getString(R.string.collect_help_expl_not_connected_to_server)));
        mCollectServerTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mCollectServerTextView);

        mODKTextView.setText(Html.fromHtml(getString(R.string.collect_help_expl_odk)));
        mODKTextView.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(mODKTextView);
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

    private void setViews() {
        toolbar = binding.toolbar;
        mCollectServerTextView = binding.serversHelp;
        mODKTextView = binding.odkHelp;
    }
}
