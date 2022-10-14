package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.dialog.ConstantsKt.IS_UPDATE_SERVER;
import static rs.readahead.washington.mobile.views.dialog.UwaziServerLanguageViewModelKt.OBJECT_KEY;
import static rs.readahead.washington.mobile.views.dialog.SharedLiveData.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.databinding.ActivityDocumentationSettingsBinding;
import rs.readahead.washington.mobile.domain.entity.Server;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IUWAZIServersPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListRefreshPresenter;
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.ServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.TellaUploadServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.UwaziServersPresenter;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;
import rs.readahead.washington.mobile.views.dialog.UwaziServerLanguageDialogFragment;
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowActivity;
import rs.readahead.washington.mobile.views.dialog.uwazi.UwaziConnectFlowActivity;
import timber.log.Timber;

@AndroidEntryPoint
public class ServersSettingsActivity extends BaseLockActivity implements
        IServersPresenterContract.IView,
        ICollectServersPresenterContract.IView,
        ITellaUploadServersPresenterContract.IView,
        ICollectBlankFormListRefreshPresenterContract.IView,
        CollectServerDialogFragment.CollectServerDialogHandler,
        UwaziServerLanguageDialogFragment.UwaziServerLanguageDialogHandler,
        IUWAZIServersPresenterContract.IView {

    private ServersPresenter serversPresenter;
    private CollectServersPresenter collectServersPresenter;
    private UwaziServersPresenter uwaziServersPresenter;
    private TellaUploadServersPresenter tellaUploadServersPresenter;
    private CollectBlankFormListRefreshPresenter refreshPresenter;
    private List<Server> servers;
    private List<TellaReportServer> tuServers;
    private List<UWaziUploadServer> uwaziServers;
    private ActivityDocumentationSettingsBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDocumentationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setStartTextTitle(getContext().getResources().getString(R.string.settings_servers_title_server_settings));
        setSupportActionBar(binding.toolbar);

        binding.toolbar.setBackClickListener(() -> {
            onBackPressed();
            return null;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.setOutlineProvider(null);
        } else {
            binding.appbar.bringToFront();
        }

        setupAutoDeleteAndMetadataUploadCheck();

        servers = new ArrayList<>();
        tuServers = new ArrayList<>();
        uwaziServers = new ArrayList<>();
        serversPresenter = new ServersPresenter(this);
        collectServersPresenter = new CollectServersPresenter(this);
        collectServersPresenter.getCollectServers();
        tellaUploadServersPresenter = new TellaUploadServersPresenter(this);
        tellaUploadServersPresenter.getTUServers();

        uwaziServersPresenter = new UwaziServersPresenter(this);
        uwaziServersPresenter.getUwaziServers();

        createRefreshPresenter();
        initUwaziEvents();
        initReportsEvents();
        initListeners();
    }

    private void initUwaziEvents() {
        INSTANCE.getCreateServer().observe(this, server -> {
            if (server != null) {
                uwaziServersPresenter.create(server);
            }
        });

        INSTANCE.getUpdateServer().observe(this, server -> {
            if (server != null) {
                uwaziServersPresenter.update(server);
            }
        });
    }

    private void initReportsEvents() {
        INSTANCE.getCreateReportsServer().observe(this, server -> {
            if (server != null) {
                tellaUploadServersPresenter.create(server);
            }
        });

        INSTANCE.getUpdateReportsServer().observe(this, server -> {
            if (server != null) {
                tellaUploadServersPresenter.update(server);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPresenting();
        stopRefreshPresenter();
    }

    private void initListeners() {
        binding.addServer.setOnClickListener((view) -> showChooseServerTypeDialog());

        binding.selectedUploadServerLayout.setOnClickListener((view) -> showChooseAutoUploadServerDialog(tuServers));
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onUwaziServersLoaded(List<UWaziUploadServer> uzServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(uzServers);
        createServerViews(servers);

        uwaziServers = uzServers;
    }

    @Override
    public void onTUServersLoaded(List<TellaReportServer> tellaReportServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(tellaReportServers);
        createServerViews(servers);

        tuServers = tellaReportServers;
       /* if (tuServers.size() > 0) {
             binding.autoUploadSwitchView.setVisibility(View.VISIBLE);
            setupAutoUploadSwitch();
            setupAutoUploadView();
        } else {
            binding.autoUploadSwitchView.setVisibility(View.GONE);
        }*/
    }

    @Override
    public void onLoadTUServersError(Throwable throwable) {
        Timber.d(throwable);
    }

    @Override
    public void onCreatedTUServer(TellaReportServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));

        tuServers.add(server);
       /* if (tuServers.size() == 1) {
            binding.autoUploadSwitchView.setVisibility(View.VISIBLE);
            setupAutoUploadSwitch();
        }

        if (tuServers.size()== 1) {
            binding.selectedUploadServerLayout.setVisibility(View.VISIBLE);
        }*/

        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    @Override
    public void onCreateTUServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_create_server), true);
    }

    @Override
    public void onCreatedUwaziServer(UWaziUploadServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        uwaziServers.add(server);
    }

    @Override
    public void onCreateUwaziServerError(Throwable throwable) {

    }

    @Override
    public void onRemovedUwaziServer(UWaziUploadServer server) {
        servers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);

    }

    @Override
    public void onRemoveUwaziServerError(Throwable throwable) {
        Timber.d(throwable);
    }

    @Override
    public void onUpdatedUwaziServer(UWaziUploadServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
        }
    }

    @Override
    public void onUpdateUwaziServerError(Throwable throwable) {
        Timber.d(throwable);
    }

    @Override
    public void onRemovedTUServer(TellaReportServer server) {
        servers.remove(server);
        binding.collectServersList.removeAllViews();

        tuServers.remove(server);
       /* if (tuServers.size() == 0) {
            binding.autoUploadSwitchView.setVisibility(View.GONE);
            binding.uploadLayout.setVisibility(View.GONE);
        }

        if (tuServers.size() == 1) {
            binding.selectedUploadServerLayout.setVisibility(View.GONE);
        }*/

        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    @Override
    public void onRemoveTUServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_delete_server), true);
    }

    @Override
    public void onUpdatedTUServer(TellaReportServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
            showToast(R.string.settings_docu_toast_server_updated);
        }
    }

    @Override
    public void onUpdateTUServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_update_server), true);
    }

    @Override
    public void onServersLoaded(List<CollectServer> collectServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(collectServers);
        createServerViews(servers);
    }

    @Override
    public void onLoadServersError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_load_list_connected_servers), true);
    }

    @Override
    public void onCreatedServer(CollectServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));

        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);

        if (MyApplication.isConnectedToInternet(this)) {
            refreshPresenter.refreshBlankForms();
        }
    }

    @Override
    public void onCreateCollectServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_create_server), true);
    }

    @Override
    public void onUpdatedServer(CollectServer server) {
        int i = servers.indexOf(server);

        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
            DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_updated), false);
        }
    }

    @Override
    public void onUpdateServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_update_server), true);
    }

    @Override
    public void onServersDeleted() {
        //Preferences.setCollectServersLayout(false);
        servers.clear();
        binding.collectServersList.removeAllViews();
        turnOffAutoUpload();
        //setupCollectSettingsView();
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_disconnect_servers_delete), false);
    }

    @Override
    public void onServersDeletedError(Throwable throwable) {
    }

    @Override
    public void onRemovedServer(CollectServer server) {
        servers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    @Override
    public void onRemoveServerError(Throwable throwable) {
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_fail_delete_server), true);
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
        BottomSheetUtils.showBinaryTypeSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_servers_add_server_dialog_title),
                getString(R.string.settings_serv_add_server_selection_dialog_title),
                getString(R.string.settings_serv_add_server_selection_dialog_description),
                getString(R.string.action_cancel), //TODO CHECk THIS
                getString(R.string.action_ok),//TODO CHECk THIS
                getString(R.string.settings_docu_add_server_dialog_select_odk),
                getString(R.string.settings_docu_add_server_dialog_select_tella_web),
                getString(R.string.settings_docu_add_server_dialog_select_tella_uwazi),
                new BottomSheetUtils.IServerChoiceActions() {
                    @Override
                    public void addUwaziServer() {
                        showUwaziServerDialog(null);
                    }

                    @Override
                    public void addTellaWebServer() {
                        showTellaUploadServerDialog(null);
                    }

                    @Override
                    public void addODKServer() {
                        showCollectServerDialog(null);
                    }
                }
        );
    }

    private void showChooseAutoUploadServerDialog(List<TellaReportServer> tellaReportServers) {

        LinkedHashMap options = new LinkedHashMap<Long, String>();
        for (Server server : tellaReportServers) {
            options.put(server.getId(), server.getName());
        }

        BottomSheetUtils.showChooseAutoUploadServerSheet(this.getSupportFragmentManager(),
                getString(R.string.settings_servers_choose_auto_upload_server_dialog_title),
                getString(R.string.settings_docu_auto_upload_server_selection_dialog_expl),
                getString(R.string.action_save),
                getString(R.string.action_cancel),
                options,
                Preferences.getAutoUploadServerId(),
                this,
                serverId -> setAutoUploadServer(serverId, (String) options.get(serverId)));
    }

    /*private void setAutoUploadServer(TellaUploadServer server) {
        serversPresenter.setAutoUploadServerId(server.getId());
        autoUploadServerName.setText(server.getName());
    }*/

    private void setAutoUploadServer(Long id, String name) {
        serversPresenter.setAutoUploadServerId(id);
        binding.serverName.setText(name);
    }

    private void editCollectServer(CollectServer server) {
        showCollectServerDialog(server);
    }

    private void editTUServer(TellaReportServer server) {
        showTellaUploadServerDialog(server);
    }

    private void editUwaziServer(UWaziUploadServer uWaziUploadServer) {
        showUwaziServerDialog(uWaziUploadServer);
    }

    private void turnOffAutoUpload() {
        binding.autoUploadSwitch.setChecked(false);
        serversPresenter.removeAutoUploadServersSettings();
        binding.uploadLayout.setVisibility(View.GONE);
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

    private void showTellaUploadServerDialog(@Nullable TellaReportServer server) {
        if (server == null) {
            startActivity(new Intent(this, ReportsConnectFlowActivity.class));
        } else {
            Intent intent = new Intent(this, ReportsConnectFlowActivity.class);
            intent.putExtra(OBJECT_KEY, new Gson().toJson(server));
            intent.putExtra(IS_UPDATE_SERVER, true);
            startActivity(intent);
        }
    }

    private void showUwaziServerDialog(@Nullable UWaziUploadServer server) {
        if (server == null) {
            startActivity(new Intent(this, UwaziConnectFlowActivity.class));
        } else {
            Intent intent = new Intent(this, UwaziConnectFlowActivity.class);
            intent.putExtra(OBJECT_KEY, new Gson().toJson(server));
            intent.putExtra(IS_UPDATE_SERVER, true);
            startActivity(intent);
        }
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

    private void setupAutoDeleteAndMetadataUploadCheck() {
        binding.autoDeleteSwitch.setChecked(Preferences.isAutoDeleteEnabled());
        binding.autoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
        binding.autoDeleteSwitch.setChecked(false);
        Preferences.setAutoDelete(false);
        return Unit.INSTANCE;
    }

    private Unit turnOnAutoDeleteSwitch() {
        binding.autoDeleteSwitch.setChecked(true);
        return Unit.INSTANCE;
    }

    private void setupCollectSettingsView() {
        if (!Preferences.isCollectServersLayout()) {
            binding.autoUploadSwitchView.setVisibility(View.GONE);
        }
    }

    private void createServerViews(List<Server> servers) {
        for (Server server : servers) {
            View view = getServerItem(server);
            binding.collectServersList.addView(view, servers.indexOf(server));
        }
    }

    private View getServerItem(Server server) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.servers_list_item, null);

        ViewGroup row = item.findViewById(R.id.server_row);
        TextView name = item.findViewById(R.id.server_title);

        if (server != null) {
            name.setText(server.getName());
            row.setOnClickListener(view -> BottomSheetUtils.showEditDeleteMenuSheet(
                    this.getSupportFragmentManager(),
                    server.getName(),
                    getString(R.string.action_edit),
                    getString(R.string.action_delete),
                    action -> {
                        if (action == BottomSheetUtils.Action.EDIT) {
                            editServer(server);
                        }
                        if (action == BottomSheetUtils.Action.DELETE) {
                            removeServer(server);
                        }
                    },
                    String.format(getResources().getString(R.string.settings_servers_delete_server_dialog_title), server.getName()),
                    getString(R.string.settings_docu_delete_server_dialog_expl),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel)
            ));
        }
        item.setTag(servers.indexOf(server));
        return item;
    }

    private void editServer(Server server) {
        switch (server.getServerType()) {
            case ODK_COLLECT:
                editCollectServer((CollectServer) server);
                break;
            case UWAZI:
                editUwaziServer((UWaziUploadServer) server);
                break;
            default:
                editTUServer((TellaReportServer) server);
                break;
        }
    }

    private void removeServer(Server server) {
        switch (server.getServerType()) {
            case ODK_COLLECT:
                collectServersPresenter.remove((CollectServer) server);
                break;
            case UWAZI:
                uwaziServersPresenter.remove((UWaziUploadServer) server);
                break;
            default:
                tellaUploadServersPresenter.remove((TellaReportServer) server);
                break;
        }
    }

    private void setupAutoUploadSwitch() {
        binding.autoUploadSwitch.setChecked(Preferences.isAutoUploadEnabled());
        binding.autoUploadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
        binding.autoUploadSwitch.setChecked(true);
        Preferences.setAutoUpload(true);
        setupAutoUploadView();
        return Unit.INSTANCE;
    }

    private Unit turnOffAutoUploadSwitch() {
        binding.autoUploadSwitch.setChecked(false);
        binding.uploadLayout.setVisibility(View.GONE);
        return Unit.INSTANCE;
    }

    private Unit turnOnAutoUploadSwitch() {
        binding.autoUploadSwitch.setChecked(true);
        binding.uploadLayout.setVisibility(View.VISIBLE);
        return Unit.INSTANCE;
    }

    private Unit disableAutoUpload() {
        binding.autoUploadSwitch.setChecked(false);
        Preferences.setAutoUpload(false);
        binding.uploadLayout.setVisibility(View.GONE);
        return Unit.INSTANCE;
    }

    private void setupAutoUploadView() {
        if (!Preferences.isAutoUploadEnabled()) {
            return;
        }

        binding.uploadLayout.setVisibility(View.VISIBLE);

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

      /*  if (tuServers.size() > 1) {
            binding.selectedUploadServerLayout.setVisibility(View.VISIBLE);
        } else {
            binding.selectedUploadServerLayout.setVisibility(View.GONE);
        }*/
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


  /*  @Override
    public void onUwaziServerDialogCreate(@Nullable UWaziUploadServer server) {
        assert server != null;
        uwaziServersPresenter.create(server);
    }

    @Override
    public void onUwaziServerDialogUpdate(@Nullable UWaziUploadServer server) {
        assert server != null;
        uwaziServersPresenter.update(server);
    }*/

    @Override
    public void onLoadUwaziServersError(@NonNull Throwable throwable) {
        Timber.d(throwable);
    }

    @Override
    public void onUwaziServerLanguageDialog(@NonNull UWaziUploadServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    @Override
    public void onDialogServerLanguageDismiss(@NonNull UWaziUploadServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
            DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_updated), false);
        }
    }

    @Override
    public void onUpdateServerLanguageDialog(@NonNull UWaziUploadServer server) {
        int i = servers.indexOf(server);

        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
            DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_updated), false);
        }
    }
}
