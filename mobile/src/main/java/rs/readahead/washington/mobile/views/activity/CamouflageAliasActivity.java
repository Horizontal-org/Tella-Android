package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.adapters.CamouflageRecycleViewAdapter;


public class CamouflageAliasActivity extends CacheWordSubscriberBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.iconsRecyclerView)
    RecyclerView recyclerView;

    private CamouflageRecycleViewAdapter adapter;
    private CamouflageManager cm = CamouflageManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camouflage_alias);

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_camo_app_bar);
        }

        adapter = new CamouflageRecycleViewAdapter();
        RecyclerView.LayoutManager galleryLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(galleryLayoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setIcons(cm.getOptions(), cm.getSelectedAliasPosition());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camouflage_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_item_select) {
            camouflage(adapter.getSelectedPosition());
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void camouflage(int position) {
        try {
            CamouflageOption option = cm.getOptions().get(position);
            if (option != null) {
                if (cm.setLauncherActivityAlias(this, option.alias)) {
                    MyApplication.bus().post(new CamouflageAliasChangedEvent()); // todo: replace with startActivityForResult
                }
            }
        } catch (IndexOutOfBoundsException ignored) {}
    }
}
