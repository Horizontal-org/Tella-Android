package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.Navigation;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kotlin.Unit;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.Server;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadServersPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListRefreshPresenter;
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.ServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.TellaUploadServersPresenter;
import rs.readahead.washington.mobile.domain.entity.ServerType;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;
import rs.readahead.washington.mobile.views.dialog.TellaUploadServerDialogFragment;
import timber.log.Timber;


public class ServersSettingsActivity extends BaseLockActivity implements
        IServersPresenterContract.IView,
        ICollectServersPresenterContract.IView,
        ITellaUploadServersPresenterContract.IView,
        ICollectBlankFormListRefreshPresenterContract.IView,
        CollectServerDialogFragment.CollectServerDialogHandler,
        TellaUploadServerDialogFragment.TellaUploadServerDialogHandler {
    //@BindView(R.id.anonymous_switch)
   // SwitchCompat anonymousSwitch;
   // @BindView(R.id.collect_switch)
   // SwitchCompat collectSwitch;
    @BindView(R.id.collect_servers_list)
    LinearLayout listView;
    //@BindView(R.id.servers_layout)
    //View serversLayout;
/*    @BindView(R.id.enable_collect_info)
    TextView collectSwitchInfo;*/
    /*@BindView(R.id.offline_mode)
    SwitchCompat offlineSwitch;
    @BindView(R.id.offline_switch_layout)
    View offlineSwitchLayout;*/
    @BindView(R.id.upload_layout)
    View autoUploadSettingsView;
    @BindView(R.id.server_name)
    TextView autoUploadServerName;
    @BindView(R.id.auto_upload_switch)
    SwitchCompat autoUploadSwitch;
    @BindView(R.id.auto_upload_switch_view)
    View autoUploadSwitchView;
    @BindView(R.id.auto_delete_switch)
    SwitchCompat autoDeleteSwitch;
    /*@BindView(R.id.metadata_check)
    AppCompatCheckBox metadataCheck;*/
    @BindView(R.id.selected_upload_server_layout)
    View serverSelectLayout;
    @BindView(R.id.activity_content_layout)
    View contentLayout;


    private ServersPresenter serversPresenter;
    private CollectServersPresenter collectServersPresenter;
    private TellaUploadServersPresenter tellaUploadServersPresenter;
    private CollectBlankFormListRefreshPresenter refreshPresenter;
    private List<Server> servers;
    private List<TellaUploadServer> tuServers;
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
            actionBar.setTitle(R.string.settings_servers_title_server_settings);
        }

        //setupAnonymousSwitch();
        //setupCollectSwitch();
        //setupOfflineSwitch();
        //setupCollectSettingsView();
        setupAutoDeleteAndMetadataUploadCheck();

        servers = new ArrayList<>();
        tuServers = new ArrayList<>();

        serversPresenter = new ServersPresenter(this);

        collectServersPresenter = new CollectServersPresenter(this);
        collectServersPresenter.getCollectServers();

        tellaUploadServersPresenter = new TellaUploadServersPresenter(this);
        tellaUploadServersPresenter.getTUServers();

        createRefreshPresenter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPresenting();
        stopRefreshPresenter();

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
        showChooseServerTypeDialog();
    }

    @OnClick(R.id.selected_upload_server_layout)
    public void chooseAutoUploadServer(View view) {
        showChooseAutoUploadServerDialog1(servers);
        //showChooseAutoUploadServerDialog(tuServers);
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onTUServersLoaded(List<TellaUploadServer> tellaUploadServers) {
        listView.removeAllViews();
        this.servers.addAll(tellaUploadServers);
        createServerViews(servers);

        tuServers = tellaUploadServers;
        /*if (tuServers.size() > 0) {
            autoUploadSwitchView.setVisibility(View.VISIBLE);
            setupAutoUploadSwitch();
            setupAutoUploadView();
        } else {
            autoUploadSwitchView.setVisibility(View.GONE);
        }*/
    }

    @Override
    public void onLoadTUServersError(Throwable throwable) {

    }

    @Override
    public void onCreatedTUServer(TellaUploadServer server) {
        servers.add(server);
        listView.addView(getServerItem(server), servers.indexOf(server));

        tuServers.add(server);
        if (tuServers.size() == 1) {
            autoUploadSwitchView.setVisibility(View.VISIBLE);
            setupAutoUploadSwitch();
        }

        if (tuServers.size() > 1) {
            serverSelectLayout.setVisibility(View.VISIBLE);
        }
        showToast(R.string.settings_docu_toast_server_created);
    }

    @Override
    public void onCreateTUServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_create_server);
    }

    @Override
    public void onRemovedTUServer(TellaUploadServer server) {
        servers.remove(server);
        listView.removeAllViews();

        tuServers.remove(server);
        if (tuServers.size() == 0) {
            autoUploadSwitchView.setVisibility(View.GONE);
            autoUploadSettingsView.setVisibility(View.GONE);
        }

        if (tuServers.size() == 1) {
            serverSelectLayout.setVisibility(View.GONE);
        }

        createServerViews(servers);
        showToast(R.string.settings_docu_toast_server_deleted);
    }

    @Override
    public void onRemoveTUServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_delete_server);
    }

    @Override
    public void onUpdatedTUServer(TellaUploadServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            listView.removeViewAt(i);
            listView.addView(getServerItem(server), i);
            showToast(R.string.settings_docu_toast_server_updated);
        }
    }

    @Override
    public void onUpdateTUServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_update_server);
    }

    @Override
    public void onServersLoaded(List<CollectServer> collectServers) {
        listView.removeAllViews();
        this.servers.addAll(collectServers);
        createServerViews(servers);

        if (collectServers.size() > 0) {
            autoUploadSwitchView.setVisibility(View.VISIBLE);
            setupAutoUploadSwitch();
            setupAutoUploadView1();
        } else {
            autoUploadSwitchView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoadServersError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_load_list_connected_servers);
    }

    @Override
    public void onCreatedServer(CollectServer server) {
        servers.add(server);
        listView.addView(getServerItem(server), servers.indexOf(server));

        showToast(R.string.settings_docu_toast_server_created);
        if (MyApplication.isConnectedToInternet(this)) {
            refreshPresenter.refreshBlankForms();
        }
    }

    @Override
    public void onCreateCollectServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_create_server);
    }

    @Override
    public void onUpdatedServer(CollectServer server) {
        int i = servers.indexOf(server);

        if (i != -1) {
            servers.set(i, server);
            listView.removeViewAt(i);
            listView.addView(getServerItem(server), i);
            showToast(R.string.settings_docu_toast_server_updated);
        }
    }

    @Override
    public void onUpdateServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_update_server);
    }

    @Override
    public void onServersDeleted() {
        //Preferences.setCollectServersLayout(false);
        servers.clear();
        listView.removeAllViews();
        turnOffAutoUpload();
       //setupCollectSettingsView();
        showToast(R.string.settings_docu_toast_disconnect_servers_delete);
    }

    @Override
    public void onServersDeletedError(Throwable throwable) {
    }

    @Override
    public void onRemovedServer(CollectServer server) {
        servers.remove(server);
        listView.removeAllViews();
        createServerViews(servers);
        showToast(R.string.settings_docu_toast_server_deleted);
    }

    @Override
    public void onRemoveServerError(Throwable throwable) {
        showToast(R.string.settings_docu_toast_fail_delete_server);
    }

    @Override
    public void onRefreshBlankFormsError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showChooseServerTypeDialog() {
       /* dialog = DialogsUtil.showServerChoosingDialog(this,
                serverType -> {
                    if (serverType == ServerType.ODK_COLLECT) {
                        showCollectServerDialog(null);
                    } else {
                        showTellaUploadServerDialog(null);
                    }
                    dialog.dismiss();
                });*/

        BottomSheetUtils.showDualChoiceTypeSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_servers_add_server_dialog_title),
                getString(R.string.settings_serv_add_server_selection_dialog_title),
                getString(R.string.settings_servers_add_server_forms),
                getString(R.string.settings_servers_add_server_reports),
                isCollectServer -> {
                    if (isCollectServer) {
                        showCollectServerDialog(null);
                    } else {
                        showTellaUploadServerDialog(null);
                    }
                });
    }

    private void showChooseAutoUploadServerDialog(List<TellaUploadServer> tellaUploadServers) {
        dialog = DialogsUtil.chooseAutoUploadServerDialog(this,
                server -> {
                    setAutoUploadServer(server.getId(),server.getName());
                    dialog.dismiss();
                },
                tellaUploadServers,
                (dialog, which) -> turnOffAutoUpload());
    }

    private void showChooseAutoUploadServerDialog1(List<Server> tellaUploadServers) {

        LinkedHashMap options = new LinkedHashMap<Long,String>();
        for (Server server : tellaUploadServers) {
            options.put(server.getId(), server.getName());
        }
        /*dialog = DialogsUtil.chooseAutoUploadServerDialog1(this,
                server -> {
                    setAutoUploadServer(server);
                    dialog.dismiss();
                },
                tellaUploadServers,
                (dialog, which) -> turnOffAutoUpload());*/
        BottomSheetUtils.showChooseAutoUploadServerSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_servers_choose_auto_upload_server_dialog_title),
                getString(R.string.settings_docu_auto_upload_server_selection_dialog_expl),
                getString(R.string.action_save),
                getString(R.string.action_cancel),
                options,
                Preferences.getAutoUploadServerId(),
                this,
                serverId -> {
                    setAutoUploadServer(serverId, (String) options.get(serverId));
                });
    }

    /*private void setAutoUploadServer(TellaUploadServer server) {
        serversPresenter.setAutoUploadServerId(server.getId());
        autoUploadServerName.setText(server.getName());
    }*/

    private void setAutoUploadServer(Long id, String name) {
        serversPresenter.setAutoUploadServerId(id);
        autoUploadServerName.setText(name);
    }

    private void editCollectServer(CollectServer server) {
        showCollectServerDialog(server);
    }

    private void editTUServer(TellaUploadServer server) {
        showTellaUploadServerDialog(server);
    }

    private void removeCollectServer(final CollectServer server) {
        dialog = DialogsUtil.showDialog(this,
                getString(R.string.settings_docu_delete_server_dialog_expl),
                getString(R.string.action_delete),
                getString(R.string.action_cancel),
                (dialog, which) -> {
                    collectServersPresenter.remove(server);
                    dialog.dismiss();
                }, null);
    }

    private void removeTUServer(final TellaUploadServer server) {
        if (server.getId() == serversPresenter.getAutoUploadServerId()) {
            dialog = DialogsUtil.showDialog(this,
                    getString(R.string.settings_docu_delete_upload_server_dialog_expl),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    (dialog, which) -> {
                        tellaUploadServersPresenter.remove(server);
                        if (server.getId() == serversPresenter.getAutoUploadServerId()) {
                            turnOffAutoUpload();
                        }
                        dialog.dismiss();
                    }, null);
        } else {
            dialog = DialogsUtil.showDialog(this,
                    getString(R.string.settings_docu_delete_auto_server_dialog_expl),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    (dialog, which) -> {
                        tellaUploadServersPresenter.remove(server);
                        dialog.dismiss();
                    }, null);
        }
    }

    private void turnOffAutoUpload() {
        autoUploadSwitch.setChecked(false);
        serversPresenter.removeAutoUploadServersSettings();
        autoUploadSettingsView.setVisibility(View.GONE);
    }

    @Override
    public void onCollectServerDialogCreate(CollectServer server) {
        collectServersPresenter.create(server);
    }

    @Override
    public void onCollectServerDialogUpdate(CollectServer server) {
        collectServersPresenter.update(server);
    }

    @Override
    public void onDialogDismiss() {

    }

    private void showCollectServerDialog(@Nullable CollectServer server) {
        CollectServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
    }

    private void showTellaUploadServerDialog(@Nullable TellaUploadServer server) {
        TellaUploadServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), TellaUploadServerDialogFragment.TAG);
    }

    private void stopPresenting() {
        if (collectServersPresenter != null) {
            collectServersPresenter.destroy();
            collectServersPresenter = null;
        }

        if (tellaUploadServersPresenter != null) {
            tellaUploadServersPresenter.destroy();
            tellaUploadServersPresenter = null;
        }

        if (serversPresenter != null) {
            serversPresenter.destroy();
            serversPresenter = null;
        }
    }

    /*private void setupAnonymousSwitch() {
        anonymousSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setAnonymousMode(!isChecked));
        anonymousSwitch.setChecked(!Preferences.isAnonymousMode());
    }*/

    private void setupAutoDeleteAndMetadataUploadCheck() {
        autoDeleteSwitch.setChecked(Preferences.isAutoDeleteEnabled());
        autoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Preferences.setAutoDelete(true);
            } else {
                BottomSheetUtils.showStandardSheet(
                        this.getSupportFragmentManager(),
                        getString(R.string.settings_servers_disable_auto_delete_dialog_title),
                        getString(R.string.settings_servers_disable_auto_delete_dialog_expl),
                        getString(R.string.action_disable),
                        getString(R.string.action_cancel),
                        this::disableAutoDelete, this::turnOnAutoDeleteSwitch);
            }
        });
        //metadataCheck.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setMetadataAutoUpload(isChecked));
        //metadataCheck.setChecked(Preferences.isMetadataAutoUpload());
    }

    private Unit disableAutoDelete() {
        autoDeleteSwitch.setChecked(false);
        Preferences.setAutoDelete(false);
        return Unit.INSTANCE;
    }

    private Unit turnOnAutoDeleteSwitch() {
        autoDeleteSwitch.setChecked(true);
        return Unit.INSTANCE;
    }

    private void setupCollectSettingsView() {
        if (Preferences.isCollectServersLayout()) {
            //collectSwitch.setChecked(true);
            //serversLayout.setVisibility(View.VISIBLE);
            //offlineSwitchLayout.setVisibility(View.VISIBLE);
        } else {
           // collectSwitch.setChecked(false);
            //serversLayout.setVisibility(View.GONE);
            //offlineSwitchLayout.setVisibility(View.GONE);
            autoUploadSwitchView.setVisibility(View.GONE);
        }
    }

    /*private void setupCollectSwitch() {
        collectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && servers.size() > 0) {
                showCollectDisableDialog();
            } else {
                //Preferences.setCollectServersLayout(isChecked);
                setupCollectSettingsView();
            }
        });
    }*/

    private void createServerViews(List<Server> servers) {
        for (Server server : servers) {
            View view = getServerItem(server);
            listView.addView(view, servers.indexOf(server));
        }
    }

    private View getServerItem(Server server) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.servers_list_item, null);

        ViewGroup row = item.findViewById(R.id.server_row);
        TextView name = item.findViewById(R.id.server_title);
        ImageView options = item.findViewById(R.id.options);

        if (server != null) {
            name.setText(server.getName());
            options.setOnClickListener(view -> showDownloadedPopupMenu(server, row, options));
            /*name.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    getContext().getResources().getDrawable(
                            server.isChecked() ? R.drawable.ic_checked_green : R.drawable.watch_later_gray
                    ),
                    null);*/
        }
        item.setTag(servers.indexOf(server));
        return item;
    }

    private void showDownloadedPopupMenu(Server server, ViewGroup row, ImageView options) {
        PopupMenu popup = new PopupMenu(row.getContext(), options);
        popup.inflate(R.menu.server_item_menu);

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.edit_server:
                    if (server.getServerType() == ServerType.ODK_COLLECT) {
                        editCollectServer((CollectServer) server);
                    } else {
                        editTUServer((TellaUploadServer) server);
                    }
                    break;
                case R.id.remove_server:
                    if (server.getServerType() == ServerType.ODK_COLLECT) {
                        removeCollectServer((CollectServer) server);
                    } else {
                        removeTUServer((TellaUploadServer) server);
                    }
                    break;
            }
            return false;
        });

        popup.show();
    }

    /*private void showCollectDisableDialog() {
        String message = getString(R.string.settings_docu_connect_to_servers_disable_dialog_expl);

        dialog = DialogsUtil.showThreeOptionDialogWithTitle(this,
                message,
                getString(R.string.settings_docu_disable_servers_dialog_title),
                getString(R.string.settings_docu_dialog_action_hide),
                getString(R.string.action_cancel),
                getString(R.string.action_delete),
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
                    serversPresenter.deleteServers();
                    dialog.dismiss();
                });
    }*/

    /*private void setupOfflineSwitch() {
        offlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setOfflineMode(isChecked));
        offlineSwitch.setChecked(Preferences.isOfflineMode());
    }

    private void setupAutoUploadSwitch() {
        autoUploadSwitch.setChecked(Preferences.isAutoUploadEnabled());
        autoUploadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoUpload(isChecked);
            if (isChecked) {
                autoUploadSettingsView.setVisibility(View.VISIBLE);
                setupAutoUploadView1();
            } else {
                autoUploadSettingsView.setVisibility(View.GONE);
            }
        });
    }*/

    private void setupAutoUploadSwitch() {
        autoUploadSwitch.setChecked(Preferences.isAutoUploadEnabled());
        autoUploadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                BottomSheetUtils.showStandardSheet(
                        this.getSupportFragmentManager(),
                        getString(R.string.settings_servers_enable_auto_upload_dialog_title),
                        getString(R.string.settings_servers_enable_auto_upload_dialog_expl),
                        getString(R.string.action_enable),
                        getString(R.string.action_cancel),
                        this::enableAutoUpload, this::turnOffAutoUploadSwitch);
            } else {
                BottomSheetUtils.showStandardSheet(
                        this.getSupportFragmentManager(),
                        getString(R.string.settings_servers_disable_auto_upload_dialog_title),
                        getString(R.string.settings_servers_disable_auto_upload_dialog_expl),
                        getString(R.string.action_disable),
                        getString(R.string.action_cancel),
                        this::disableAutoUpload, this::turnOnAutoUploadSwitch);
            }
        });
    }

    private Unit enableAutoUpload() {
        autoUploadSwitch.setChecked(true);
        Preferences.setAutoUpload(true);
        setupAutoUploadView1();
        return Unit.INSTANCE;
    }

    private Unit turnOffAutoUploadSwitch() {
        autoUploadSwitch.setChecked(false);
        autoUploadSettingsView.setVisibility(View.GONE);
        return Unit.INSTANCE;
    }

    private Unit turnOnAutoUploadSwitch() {
        autoUploadSwitch.setChecked(true);
        autoUploadSettingsView.setVisibility(View.VISIBLE);
        return Unit.INSTANCE;
    }

    private Unit disableAutoUpload() {
        autoUploadSwitch.setChecked(false);
        Preferences.setAutoUpload(false);
        autoUploadSettingsView.setVisibility(View.GONE);
        return Unit.INSTANCE;
    }

    private void setupAutoUploadView() {
        if (!Preferences.isAutoUploadEnabled()) {
            return;
        }

        autoUploadSettingsView.setVisibility(View.VISIBLE);

        if (serversPresenter.getAutoUploadServerId() == -1) {  // check if auto upload server is set
            if (tuServers.size() == 1) {
                setAutoUploadServer(tuServers.get(0).getId(), tuServers.get(0).getName());
            } else {
                showChooseAutoUploadServerDialog(tuServers);
            }
        } else {
            for (int i = 0; i < tuServers.size(); i++) {
                if (tuServers.get(i).getId() == serversPresenter.getAutoUploadServerId()) {
                    setAutoUploadServer(tuServers.get(i).getId(), tuServers.get(i).getName());
                    break;
                }
            }
        }

        if (tuServers.size() > 1) {
            serverSelectLayout.setVisibility(View.VISIBLE);
        } else {
            serverSelectLayout.setVisibility(View.GONE);
        }
    }

    private void setupAutoUploadView1() {
        if (!Preferences.isAutoUploadEnabled()) {
            return;
        }

        autoUploadSettingsView.setVisibility(View.VISIBLE);

        if (serversPresenter.getAutoUploadServerId() == -1) {  // check if auto upload server is set
            if (servers.size() == 1) {
                //setAutoUploadServer(tuServers.get(0).getId(), tuServers.get(0).getName());
                setAutoUploadServer(servers.get(0).getId(), servers.get(0).getName());
            } else {
                showChooseAutoUploadServerDialog1(servers);
            }
        } else {
            for (int i = 0; i < servers.size(); i++) {
                if (servers.get(i).getId() == serversPresenter.getAutoUploadServerId()) {
                    //setAutoUploadServer(tuServers.get(i).getId(), tuServers.get(i).getName());
                    setAutoUploadServer(servers.get(i).getId(), servers.get(i).getName());
                    break;
                }
            }
        }

        if (servers.size() > 1) {
            serverSelectLayout.setVisibility(View.VISIBLE);
        } else {
            serverSelectLayout.setVisibility(View.GONE);
        }
    }

    private void stopRefreshPresenter() {
        if (refreshPresenter != null) {
            refreshPresenter.destroy();
            refreshPresenter = null;
        }
    }

    private void createRefreshPresenter() {
        if (refreshPresenter == null) {
            refreshPresenter = new CollectBlankFormListRefreshPresenter(this);
        }
    }

    @Override
    public void onTellaUploadServerDialogCreate(TellaUploadServer server) {
        tellaUploadServersPresenter.create(server);
    }

    @Override
    public void onTellaUploadServerDialogUpdate(TellaUploadServer server) {
        tellaUploadServersPresenter.update(server);
    }
}
