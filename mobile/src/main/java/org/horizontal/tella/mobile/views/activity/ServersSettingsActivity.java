package org.horizontal.tella.mobile.views.activity;

import static org.horizontal.tella.mobile.views.dialog.ConstantsKt.IS_UPDATE_SERVER;
import static org.horizontal.tella.mobile.views.dialog.SharedLiveData.INSTANCE;
import static org.horizontal.tella.mobile.views.dialog.UwaziServerLanguageViewModelKt.OBJECT_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.hzontal.utils.Util;

import org.horizontal.tella.mobile.mvvm.settings.CollectServersViewModel;
import org.horizontal.tella.mobile.mvvm.settings.GoogleDriveServersViewModel;
import org.horizontal.tella.mobile.mvvm.settings.TellaUploadServersViewModel;
import org.horizontal.tella.mobile.mvvm.settings.UwaziServersViewModel;
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.databinding.ActivityDocumentationSettingsBinding;
import org.horizontal.tella.mobile.domain.entity.Server;
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer;
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer;
import org.horizontal.tella.mobile.domain.entity.googledrive.Config;
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;
import org.horizontal.tella.mobile.mvp.contract.IDropBoxServersPresenterContract;
import org.horizontal.tella.mobile.mvp.contract.INextCloudServersPresenterContract;
import org.horizontal.tella.mobile.mvp.contract.IServersPresenterContract;
import org.horizontal.tella.mobile.mvp.presenter.CollectBlankFormListRefreshPresenter;
import org.horizontal.tella.mobile.mvp.presenter.DropBoxServersPresenter;
import org.horizontal.tella.mobile.mvp.presenter.NextCloudServersPresenter;
import org.horizontal.tella.mobile.mvp.presenter.ServersPresenter;
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity;
import org.horizontal.tella.mobile.views.dialog.CollectServerDialogFragment;
import org.horizontal.tella.mobile.views.dialog.UwaziServerLanguageDialogFragment;
import org.horizontal.tella.mobile.views.dialog.nextcloud.NextCloudLoginFlowActivity;
import org.horizontal.tella.mobile.views.dialog.dropbox.DropBoxConnectFlowActivity;
import org.horizontal.tella.mobile.views.dialog.googledrive.GoogleDriveConnectFlowActivity;
import org.horizontal.tella.mobile.views.dialog.reports.ReportsConnectFlowActivity;
import org.horizontal.tella.mobile.views.dialog.uwazi.UwaziConnectFlowActivity;

import timber.log.Timber;

@AndroidEntryPoint
public class ServersSettingsActivity extends BaseLockActivity implements IServersPresenterContract.IView, ICollectBlankFormListRefreshPresenterContract.IView, CollectServerDialogFragment.CollectServerDialogHandler, UwaziServerLanguageDialogFragment.UwaziServerLanguageDialogHandler, IDropBoxServersPresenterContract.IView, INextCloudServersPresenterContract.IView {

    private ServersPresenter serversPresenter;
    private CollectServersViewModel collectServersViewModel;
    private UwaziServersViewModel uwaziServersViewModel;
    private GoogleDriveServersViewModel googleDriveViewModel;
    private TellaUploadServersViewModel tellaUploadServersViewModel;
    private CollectBlankFormListRefreshPresenter refreshPresenter;
    private DropBoxServersPresenter dropBoxServersPresenter;
    private NextCloudServersPresenter nextCloudServersPresenter;
    private List<Server> servers;
    private List<TellaReportServer> tuServers;
    private List<UWaziUploadServer> uwaziServers;
    private List<GoogleDriveServer> googleDriveServers;
    private List<DropBoxServer> dropBoxServers;
    private List<NextCloudServer> nextCloudServers;
    private ActivityDocumentationSettingsBinding binding;

    @Inject
    public Config config;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDocumentationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setStartTextTitle(getResources().getString(R.string.settings_servers_title_server_settings2));
        setSupportActionBar(binding.toolbar);

        googleDriveViewModel = new ViewModelProvider(this).get(GoogleDriveServersViewModel.class);
        uwaziServersViewModel = new ViewModelProvider(this).get(UwaziServersViewModel.class);
        collectServersViewModel = new ViewModelProvider(this).get(CollectServersViewModel.class);
        tellaUploadServersViewModel = new ViewModelProvider(this).get(TellaUploadServersViewModel.class);

        binding.toolbar.setOnRightClickListener(() -> {
            maybeChangeTemporaryTimeout(() -> {
                Util.startBrowserIntent(this, getString(R.string.config_connections_url));
                return null;
            });
            return null;
        });

        binding.toolbar.setBackClickListener(() -> {
            onBackPressed();
            return null;
        });

        binding.appbar.setOutlineProvider(null);

        setupAutoDeleteAndMetadataUploadCheck();

        servers = new ArrayList<>();
        tuServers = new ArrayList<>();
        uwaziServers = new ArrayList<>();
        googleDriveServers = new ArrayList<>();
        dropBoxServers = new ArrayList<>();
        nextCloudServers = new ArrayList<>();
        serversPresenter = new ServersPresenter(this);
        collectServersViewModel.getCollectServers();
        tellaUploadServersViewModel.getTellaUploadServers();

        uwaziServersViewModel.getUwaziServers();

        googleDriveViewModel.getGoogleDriveServers(config.getGoogleClientId());

        dropBoxServersPresenter = new DropBoxServersPresenter(this);
        dropBoxServersPresenter.getDropBoxServers();

        //NextCloud server
        nextCloudServersPresenter = new NextCloudServersPresenter(this);
        nextCloudServersPresenter.getNextCloudServers();
        createRefreshPresenter();
        initObservers();
        initUwaziEvents();
        initReportsEvents();
        initGoogleDriveEvents();
        initDropBoxEvents();
        initNextCloudEvents();
        initListeners();
    }

    private void initObservers() {
        //Google drive connection
        googleDriveViewModel.getGoogleDriveServers().observe(this, this::onGoogleDriveServersLoaded);
        googleDriveViewModel.getError().observe(this, this::showConnectionsError);
        googleDriveViewModel.getCreatedServer().observe(this, this::onCreatedGoogleDriveServer);
        googleDriveViewModel.getRemovedServer().observe(this, this::onRemovedGoogleDriveServer);

        //Uwazi connection
        uwaziServersViewModel.getListUwaziServers().observe(this, this::onUwaziServersLoaded);
        uwaziServersViewModel.getError().observe(this, this::showConnectionsError);
        uwaziServersViewModel.getCreatedServer().observe(this, this::onCreatedUwaziServer);
        uwaziServersViewModel.getRemovedServer().observe(this, this::onRemovedUwaziServer);
        uwaziServersViewModel.getUpdatedServer().observe(this, this::onUpdatedUwaziServer);

        //collect ODK connection
        collectServersViewModel.getListCollectServers().observe(this, this::onServersLoaded);
        collectServersViewModel.getError().observe(this, this::showConnectionsError);
        collectServersViewModel.getCreatedServer().observe(this, this::onCreatedServer);
        collectServersViewModel.getRemovedServer().observe(this, this::onRemovedServer);
        collectServersViewModel.getUpdatedServer().observe(this, this::onUpdatedServer);

        //Tella upload server
        tellaUploadServersViewModel.getListTellaReportServers().observe(this, this::onTUServersLoaded);
        tellaUploadServersViewModel.getError().observe(this, this::showConnectionsError);
        tellaUploadServersViewModel.getCreatedServer().observe(this, this::onCreatedTUServer);
        tellaUploadServersViewModel.getRemovedServer().observe(this, this::onRemovedTUServer);
        tellaUploadServersViewModel.getUpdatedServer().observe(this, this::onUpdatedTUServer);
    }

    private void initDropBoxEvents() {
        INSTANCE.getCreateDropBoxServer().observe(this, server -> {
            if (server != null) {
                dropBoxServersPresenter.create(server);
            }
        });
    }

    private void initUwaziEvents() {
        INSTANCE.getCreateServer().observe(this, server -> {
            if (server != null) {
                uwaziServersViewModel.create(server);
            }
        });

        INSTANCE.getUpdateServer().observe(this, server -> {
            if (server != null) {
                uwaziServersViewModel.update(server);
            }
        });
    }

    private void initReportsEvents() {
        INSTANCE.getCreateReportsServer().observe(this, server -> {
            if (server != null) {
                tellaUploadServersViewModel.create(server);
            }
        });

        INSTANCE.getCreateReportsServerAndCloseActivity().observe(this, server -> {
            if (server != null) {
                tellaUploadServersViewModel.create(server);
            }
        });

        INSTANCE.getUpdateReportsServer().observe(this, server -> {
            if (server != null) {
                tellaUploadServersViewModel.update(server);
            }
        });
    }

    private void initGoogleDriveEvents() {
        INSTANCE.getCreateGoogleDriveServer().observe(this, server -> {
            if (server != null) {
                googleDriveViewModel.create(server);
            }
        });
    }

    private void initNextCloudEvents() {
        INSTANCE.getCreateNextCloudServer().observe(this, server -> {
            if (server != null) {
                nextCloudServersPresenter.create(server);
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

    public void onUwaziServersLoaded(List<UWaziUploadServer> uzServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(uzServers);
        createServerViews(servers);

        uwaziServers = uzServers;
    }

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

    public void onCreatedTUServer(TellaReportServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        tuServers.add(server);
        saveAutoUploadServer(server);
        Preferences.setAutoUpload(isAutoUploadEnabled(servers));
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    private void onCreatedUwaziServer(UWaziUploadServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        uwaziServers.add(server);
    }

    public void onRemovedUwaziServer(UWaziUploadServer server) {
        servers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    public void onUpdatedUwaziServer(UWaziUploadServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
        }
    }

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
        if (tuServers.isEmpty()) {
            Preferences.setAutoUpload(false);
            Preferences.setAutoDelete(false);
        }

    }

    public void onUpdatedTUServer(TellaReportServer server) {
        int i = servers.indexOf(server);
        saveAutoUploadServer(server);
        Preferences.setAutoDelete(server.isAutoDelete());
        if (server.isAutoUpload()) {
            Preferences.setAutoUploadServerId(server.getId());
        }
        if (i != -1) {
            servers.set(i, server);
            Preferences.setAutoUpload(isAutoUploadEnabled(servers));
            binding.collectServersList.removeViewAt(i);
            binding.collectServersList.addView(getServerItem(server), i);
            DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_updated), false);
        }
    }

    private boolean isAutoUploadEnabled(List<Server> servers) {
        return servers.stream().filter(server -> server instanceof TellaReportServer).map(server -> (TellaReportServer) server).anyMatch(TellaReportServer::isAutoUpload);
    }

    public void onServersLoaded(List<CollectServer> collectServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(collectServers);
        createServerViews(servers);
    }

    public void onCreatedServer(CollectServer server) {
        servers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));

        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);

        if (MyApplication.isConnectedToInternet(this)) {
            refreshPresenter.refreshBlankForms();
        }
    }

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

    public void onRemovedServer(CollectServer server) {
        servers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
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

        BottomSheetUtils.showBinaryTypeSheet(this.getSupportFragmentManager(), this, getString(R.string.settings_add_server_selection_dialog_title), getString(R.string.settings_add_server_selection_dialog_title), getString(R.string.Connections_description_selection), getString(R.string.Connections_description), this::browseIntent, getString(R.string.action_cancel), //TODO CHECk THIS
                getString(R.string.action_ok),
                getString(R.string.settings_docu_add_server_dialog_select_odk), getString(R.string.settings_docu_add_server_dialog_select_tella_web), getString(R.string.settings_docu_add_server_dialog_select_tella_uwazi), getString(R.string.settings_docu_add_server_dialog_select_tella_google_drive), getString(R.string.settings_docu_add_server_dialog_select_tella_dropbox), getString(R.string.settings_docu_add_server_dialog_select_next_cloud), getString(R.string.unavailable_connections), getString(R.string.unavailable_connections_desc), servers.stream().anyMatch(server -> server instanceof GoogleDriveServer), servers.stream().anyMatch(server -> server instanceof DropBoxServer), servers.stream().anyMatch(server -> server instanceof NextCloudServer), new BottomSheetUtils.IServerChoiceActions() {

                    @Override
                    public void addDropBoxServer() {
                        showDropBoxServerDialog(null);
                    }

                    @Override
                    public void addGoogleDriveServer() {
                        showGoogleDriveServerDialog(null);
                    }

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

                    @Override
                    public void addNextCloudServer() {
                        showNexCloudDialog(null);
                    }

                });
    }

    private void showChooseAutoUploadServerDialog(List<TellaReportServer> tellaReportServers) {

        LinkedHashMap options = new LinkedHashMap<Long, String>();
        for (Server server : tellaReportServers) {
            options.put(server.getId(), server.getName());
        }

        BottomSheetUtils.showChooseAutoUploadServerSheet(this.getSupportFragmentManager(), getString(R.string.settings_servers_choose_auto_upload_server_dialog_title), getString(R.string.settings_docu_auto_upload_server_selection_dialog_expl), getString(R.string.action_save), getString(R.string.action_cancel), options, Preferences.getAutoUploadServerId(), this, serverId -> setAutoUploadServer(serverId, (String) options.get(serverId)));
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
        collectServersViewModel.create(server);
    }

    @Override
    public void onCollectServerDialogUpdate(CollectServer server) {
        collectServersViewModel.update(server);
    }

    @Override
    public void onDialogDismiss() {

    }

    private void showCollectServerDialog(@Nullable CollectServer server) {
        CollectServerDialogFragment.newInstance(server).show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
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

    private void showNexCloudDialog(@Nullable NextCloudServer server) {
        if (server == null) {
            startActivity(new Intent(this, NextCloudLoginFlowActivity.class));
        }
    }


    private void showGoogleDriveServerDialog(@Nullable GoogleDriveServer googleDriveServer) {
        if (googleDriveServer == null) {
            startActivity(new Intent(this, GoogleDriveConnectFlowActivity.class));
        }
    }

    private void showDropBoxServerDialog(@Nullable DropBoxServer dropBoxServer) {
        if (dropBoxServer == null) {
            Intent intent = new Intent(this, DropBoxConnectFlowActivity.class);
            startActivity(intent);
        }
    }

    private void stopPresenting() {

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
                BottomSheetUtils.showStandardSheet(this.getSupportFragmentManager(), getString(R.string.settings_servers_disable_auto_delete_dialog_title), getString(R.string.settings_servers_disable_auto_delete_dialog_expl), getString(R.string.action_disable), getString(R.string.action_cancel), this::disableAutoDelete, this::turnOnAutoDeleteSwitch);
            }
        });
        //metadataCheck.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setMetadataAutoUpload(isChecked));
        //metadataCheck.setChecked(Preferences.isMetadataAutoUpload());
    }

    private Unit browseIntent() {
        maybeChangeTemporaryTimeout(() -> {
            Util.startBrowserIntent(this, getString(R.string.config_organizations_url));
            return null;
        });
        return Unit.INSTANCE;
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
        @SuppressLint("InflateParams") LinearLayout item = (LinearLayout) inflater.inflate(R.layout.servers_list_item, null);

        ViewGroup row = item.findViewById(R.id.server_row);
        TextView name = item.findViewById(R.id.server_title);

        if (server != null) {
            name.setText(server.getName());
            row.setOnClickListener(view -> BottomSheetUtils.showEditDeleteMenuSheet(this.getSupportFragmentManager(), server.getName(), getString(R.string.action_edit), getString(R.string.action_delete), action -> {
                if (action == BottomSheetUtils.Action.EDIT) {
                    editServer(server);
                }
                if (action == BottomSheetUtils.Action.DELETE) {
                    removeServer(server);
                }
            }, String.format(getResources().getString(R.string.settings_servers_delete_server_dialog_title), server.getName()), getString(R.string.settings_docu_delete_server_dialog_expl), getString(R.string.action_delete), getString(R.string.action_cancel), -1, !(server instanceof GoogleDriveServer || server instanceof DropBoxServer || server instanceof NextCloudServer)));
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
            case GOOGLE_DRIVE:
                // editGoogleDriveServer((GoogleDriveServer) server);
            case DROP_BOX:
                // editGoogleDriveServer((GoogleDriveServer) server);
            case NEXTCLOUD:
            default:
                editTUServer((TellaReportServer) server);
                break;
        }
    }

    private void removeServer(Server server) {
        switch (server.getServerType()) {
            case ODK_COLLECT:
                collectServersViewModel.remove((CollectServer) server);
                break;
            case UWAZI:
                uwaziServersViewModel.remove((UWaziUploadServer) server);
                break;
            case GOOGLE_DRIVE:
                googleDriveViewModel.remove((GoogleDriveServer) server);
                break;
            case DROP_BOX:
                dropBoxServersPresenter.remove((DropBoxServer) server);
                break;
            case NEXTCLOUD:
                nextCloudServersPresenter.remove((NextCloudServer) server);
                break;
            default:
                tellaUploadServersViewModel.remove((TellaReportServer) server);
                break;
        }
    }

    private void setupAutoUploadSwitch() {
        binding.autoUploadSwitch.setChecked(Preferences.isAutoUploadEnabled());
        binding.autoUploadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                BottomSheetUtils.showStandardSheet(this.getSupportFragmentManager(), getString(R.string.settings_servers_enable_auto_upload_dialog_title), getString(R.string.settings_servers_enable_auto_upload_dialog_expl), getString(R.string.action_enable), getString(R.string.action_cancel), this::enableAutoUpload, this::turnOffAutoUploadSwitch);
            } else {
                BottomSheetUtils.showStandardSheet(this.getSupportFragmentManager(), getString(R.string.settings_servers_disable_auto_upload_dialog_title), getString(R.string.settings_servers_disable_auto_upload_dialog_expl), getString(R.string.action_disable), getString(R.string.action_cancel), this::disableAutoUpload, this::turnOnAutoUploadSwitch);
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

    private void saveAutoUploadServer(TellaReportServer server) {
        if (server.isActivatedBackgroundUpload()) {
            Preferences.setAutoUploadServerId(server.getId());
        }
    }

    public void onGoogleDriveServersLoaded(@NonNull List<GoogleDriveServer> googleDriveServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(googleDriveServers);
        createServerViews(servers);
        this.googleDriveServers = googleDriveServers;
    }

    public void onCreatedGoogleDriveServer(@NonNull GoogleDriveServer server) {
        servers.add(server);
        googleDriveServers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    public void onRemovedGoogleDriveServer(@NonNull GoogleDriveServer server) {
        servers.remove(server);
        googleDriveServers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    private void showConnectionsError(int error) {
        DialogUtils.showBottomMessage(this, getString(error), true);
    }

    @Override
    public void onDropBoxServersLoaded(@NonNull List<DropBoxServer> dropBoxServerServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(dropBoxServerServers);
        createServerViews(servers);
        this.dropBoxServers = dropBoxServerServers;
    }

    @Override
    public void onLoadDropBoxServersError(@NonNull Throwable throwable) {

    }

    @Override
    public void onCreatedDropBoxServer(@NonNull DropBoxServer server) {
        servers.add(server);
        dropBoxServers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    @Override
    public void onCreateDropBoxServerError(@NonNull Throwable throwable) {

    }

    @Override
    public void onRemovedDropBoxServer(@NonNull DropBoxServer server) {
        servers.remove(server);
        dropBoxServers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    @Override
    public void onRemoveDropBoxServerError(@NonNull Throwable throwable) {

    }

    @Override
    public void onNextCloudServersLoaded(@NonNull List<NextCloudServer> nextCloudServers) {
        binding.collectServersList.removeAllViews();
        this.servers.addAll(nextCloudServers);
        createServerViews(servers);

        this.nextCloudServers = nextCloudServers;
    }

    @Override
    public void onLoadNextCloudServersError(@NonNull Throwable throwable) {

    }

    @Override
    public void onCreatedNextCloudServer(@NonNull NextCloudServer server) {
        servers.add(server);
        nextCloudServers.add(server);
        binding.collectServersList.addView(getServerItem(server), servers.indexOf(server));
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_created), false);
    }

    @Override
    public void onCreateNextCloudServerError(@NonNull Throwable throwable) {

    }

    @Override
    public void onRemovedNextCloudServer(@NonNull NextCloudServer server) {
        servers.remove(server);
        nextCloudServers.remove(server);
        binding.collectServersList.removeAllViews();
        createServerViews(servers);
        DialogUtils.showBottomMessage(this, getString(R.string.settings_docu_toast_server_deleted), false);
    }

    @Override
    public void onRemoveNextCloudServerError(@NonNull Throwable throwable) {

    }
}
