package rs.readahead.washington.mobile.views.activity;

import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;
import rs.readahead.washington.mobile.util.CamouflageManager;
import rs.readahead.washington.mobile.views.adapters.CamouflageRecycleViewAdapter;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;


public class CamouflageAliasActivity extends BaseLockActivity {
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

        //showAddCamouflageDialog();
        //showChooseCamouflageTypeDialog();
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

    private void showAddCamouflageDialog() {
        BottomSheetUtils.showDualChoiceTypeSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_prot_select_camouflage),
                getString(R.string.settings_servers_add_camouflage_subtitle),
                getString(R.string.settings_servers_change_camouflage_method),
                getString(R.string.settings_servers_remove_camouflage_method),
                option -> {
                    if (option) {

                    } else {

                    }
                });
    }

    private void showChooseCamouflageTypeDialog() {
        BottomSheetUtils.showChangeCamouflageSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_prot_select_camouflage),
                getString(R.string.settings_servers_setup_camouflage_title),
                getString(R.string.settings_servers_setup_camouflage_description),
                getString(R.string.settings_servers_setup_change_camouflage_title),
                getString(R.string.settings_servers_setup_change_camouflage_subtitle),
                getString(R.string.settings_servers_setup_hide_behind_camouflage_title),
                getString(R.string.settings_servers_setup_hide_behind_camouflage_subtitle),
                option -> {
                    if (option) {

                    } else {

                    }
                });
    }
}
