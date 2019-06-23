package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;


public class DocumentationSettingsActivity extends CacheWordSubscriberBaseActivity implements
        ICollectServersPresenterContract.IView,
        CollectServerDialogFragment.CollectServerDialogHandler {
    @BindView(R.id.anonymous_switch)
    SwitchCompat anonymousSwitch;
    @BindView(R.id.collect_switch)
    SwitchCompat collectSwitch;
    @BindView(R.id.collect_servers_list)
    LinearLayout listView;
    /*@BindView(R.id.collect_servers_info)
    TextView collectServersInfo;*/
    @BindView(R.id.servers_layout)
    View serversLayout;
    @BindView(R.id.enable_collect_info)
    TextView collectSwitchInfo;
    @BindView(R.id.offline_mode)
    SwitchCompat offlineSwitch;
    @BindView(R.id.offline_switch_layout)
    View offlineSwitchLayout;

    private CollectServersPresenter presenter;
    List<CollectServer> servers;
    private AlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_documentation_settings);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.documentation);
        }

        setupAnonymousSwitch();
        setupCollectSwitch();
        setupOfflineSwitch();
        setupCollectSettingsView();

        /*collectServersInfo.setText(Html.fromHtml(getString(R.string.manage_collect_servers)));
        collectServersInfo.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(collectServersInfo);*/

        collectSwitchInfo.setText(Html.fromHtml(getString(R.string.enable_collect_info)));
        collectSwitchInfo.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(collectSwitchInfo);

        servers = new ArrayList<>();

        presenter = new CollectServersPresenter(this);
        presenter.getServers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPresenting();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.add_server)
    public void manage(View view) {
        showCollectServerDialog(null);
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onServersLoaded(List<CollectServer> servers) {
        listView.removeAllViews();
        this.servers = servers;
        createServerViews(servers);
    }

    @Override
    public void onLoadServersError(Throwable throwable) {
        showToast(R.string.ra_collect_server_load_error);
    }

    @Override
    public void onCreatedServer(CollectServer server) {
        servers.add(server);
        listView.addView(getServerItem(server), servers.indexOf(server));
        showToast(R.string.ra_server_created);
    }

    @Override
    public void onCreateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_create_error);
    }

    @Override
    public void onUpdatedServer(CollectServer server) {
        int i = servers.indexOf(server);

        if (i != -1) {
            servers.set(i, server);
            listView.removeViewAt(i);
            listView.addView(getServerItem(server), i);
            showToast(R.string.ra_server_updated);
        }
    }

    @Override
    public void onUpdateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_update_error);
    }

    @Override
    public void onCollectDeleted() {
        Preferences.setCollectServersLayout(false);
        setupCollectSettingsView();
        showToast(R.string.deleted_servers_and_forms);
    }

    @Override
    public void onCollectDeletedError(Throwable throwable) {

    }

    @Override
    public void onRemovedServer(CollectServer server) {
        servers.remove(server);
        //listView.removeViewAt(servers.indexOf(server));
        listView.removeAllViews();
        createServerViews(servers);
        showToast(R.string.ra_server_removed);
    }

    @Override
    public void onRemoveServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_remove_error);
    }

    @Override
    public Context getContext() {
        return this;
    }


    private void editCollectServer(CollectServer server) {
        showCollectServerDialog(server);
    }


    private void removeCollectServer(final CollectServer server) {
        dialog = DialogsUtil.showDialog(this,
                getString(R.string.ra_server_remove_confirmation_text),
                getString(R.string.ra_remove),
                getString(R.string.cancel),
                (dialog, which) -> {
                    presenter.remove(server);
                    dialog.dismiss();
                }, null);
    }

    @Override
    public void onCollectServerDialogCreate(CollectServer server) {
        presenter.create(server);
    }

    @Override
    public void onCollectServerDialogUpdate(CollectServer server) {
        presenter.update(server);
    }

    private void showCollectServerDialog(@Nullable CollectServer server) {
        CollectServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
    }

    private void stopPresenting() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void setupAnonymousSwitch() {
        anonymousSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setAnonymousMode(!isChecked));
        anonymousSwitch.setChecked(!Preferences.isAnonymousMode());
    }

    private void setupCollectSettingsView() {
        if (Preferences.isCollectServersLayout()) {
            collectSwitch.setChecked(true);
            serversLayout.setVisibility(View.VISIBLE);
            offlineSwitchLayout.setVisibility(View.VISIBLE);
        } else {
            collectSwitch.setChecked(false);
            serversLayout.setVisibility(View.GONE);
            offlineSwitchLayout.setVisibility(View.GONE);
        }
    }

    private void setupCollectSwitch() {
        collectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && servers.size() > 0) {
                showCollectDisableDialog();
            } else {
                Preferences.setCollectServersLayout(isChecked);
                setupCollectSettingsView();
            }
        });
    }

    private void createServerViews(List<CollectServer> servers) {
        for (CollectServer collectServer : servers) {
            View view = getServerItem(collectServer);
            listView.addView(view, servers.indexOf(collectServer));
        }
    }

    private View getServerItem(CollectServer collectServer) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.collect_server_row_for_list, null);

        TextView name = ButterKnife.findById(item, R.id.server_title);
        ImageView edit = ButterKnife.findById(item, R.id.edit);
        ImageView remove = ButterKnife.findById(item, R.id.delete);

        if (collectServer != null) {
            name.setText(collectServer.getName());

            name.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    getContext().getResources().getDrawable(
                            collectServer.isChecked() ? R.drawable.ic_checked_green : R.drawable.watch_later_gray
                    ),
                    null);

            remove.setOnClickListener(v -> removeCollectServer(collectServer));
            edit.setOnClickListener(v -> editCollectServer(collectServer));
        }
        item.setTag(servers.indexOf(collectServer));
        return item;
    }

    private void showCollectDisableDialog() {
        String message = getString(R.string.disable_collect_info);

        dialog = DialogsUtil.showThreeOptionDialogWithTitle(this,
                message,
                getString(R.string.disable_collect),
                getString(R.string.hide),
                getString(R.string.cancel),
                getString(R.string.delete),
                (dialog, which) -> {  //hide
                    Preferences.setCollectServersLayout(false);
                    setupCollectSettingsView();
                    dialog.dismiss();
                },
                (dialog, which) -> {  //cancel
                    collectSwitch.setChecked(true);
                    dialog.dismiss();
                },
                (dialog, which) -> {   //delete
                    presenter.deleteCollect();
                    dialog.dismiss();
                });
    }

    private void setupOfflineSwitch() {
        offlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setOfflineMode(isChecked));
        offlineSwitch.setChecked(Preferences.isOfflineMode());
    }
}
