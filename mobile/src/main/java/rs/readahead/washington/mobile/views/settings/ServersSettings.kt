package rs.readahead.washington.mobile.views.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadServersPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListRefreshPresenter
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter
import rs.readahead.washington.mobile.mvp.presenter.ServersPresenter
import rs.readahead.washington.mobile.mvp.presenter.TellaUploadServersPresenter
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment.CollectServerDialogHandler
import rs.readahead.washington.mobile.views.dialog.TellaUploadServerDialogFragment
import rs.readahead.washington.mobile.views.dialog.TellaUploadServerDialogFragment.TellaUploadServerDialogHandler
import timber.log.Timber
import java.util.*


class ServersSettings : BaseFragment(),
        IServersPresenterContract.IView,
ICollectServersPresenterContract.IView,
ITellaUploadServersPresenterContract.IView,
ICollectBlankFormListRefreshPresenterContract.IView,
CollectServerDialogHandler,
TellaUploadServerDialogHandler {
    var listView: LinearLayout? = null
    var servers = ArrayList<Server?>()
    var tuServers = ArrayList<TellaUploadServer?>()

    private var serversPresenter: ServersPresenter? = null
    private var collectServersPresenter: CollectServersPresenter? = null
    private var tellaUploadServersPresenter: TellaUploadServersPresenter? = null
    private var refreshPresenter: CollectBlankFormListRefreshPresenter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_servers_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_title_server_settings)

        val addServerButton = view.findViewById<LinearLayout>(R.id.add_servers)
        listView = view.findViewById(R.id.collect_servers_list)
        /*addServerButton.setOnClickListener {
            activity?.let {
                BottomSheetUtils.showDualChoiceTypeSheet(it.supportFragmentManager,
                    "Add Server",
                    descriptionText = "What type of server?",
                    buttonOneLabel = "FORMS (OPEN DATA KIT)",
                    buttonTwoLabel = "REPORTS (DIRECT UPLOAD)",
                    onActionOneClick = {showCollectServerDialog(null)},
                    onActionTwoClick = {showTellaUploadServerDialog(null)},
                    onCancelClick = {})
            }
        }*/

        servers = ArrayList<Server?>()
        tuServers = ArrayList<TellaUploadServer?>()

        return view
    }

    override fun initView(view: View) {
        TODO("Not yet implemented")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        serversPresenter = ServersPresenter(this)

        collectServersPresenter = CollectServersPresenter(this)
        collectServersPresenter!!.getCollectServers()

        tellaUploadServersPresenter = TellaUploadServersPresenter(this)
        tellaUploadServersPresenter!!.getTUServers()

        createRefreshPresenter()

    }

    private fun showCollectServerDialog(server: CollectServer?) {
        activity?.let {
            CollectServerDialogFragment.newInstance(server)
                    .show(it.supportFragmentManager, CollectServerDialogFragment.TAG)
        }
    }

    private fun showTellaUploadServerDialog(server: TellaUploadServer?) {
        activity?.let {
            TellaUploadServerDialogFragment.newInstance(server)
                    .show(it.supportFragmentManager, TellaUploadServerDialogFragment.TAG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPresenting()
       /* stopRefreshPresenter()
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss()
        }*/
    }

    private fun stopPresenting() {
        if (collectServersPresenter != null) {
            collectServersPresenter!!.destroy()
            collectServersPresenter = null
        }
        if (tellaUploadServersPresenter != null) {
            tellaUploadServersPresenter!!.destroy()
            tellaUploadServersPresenter = null
        }
        if (serversPresenter != null) {
            serversPresenter!!.destroy()
            serversPresenter = null
        }
    }

    override fun onRemoveTUServerError(throwable: Throwable?) {
        //showToast(R.string.settings_docu_toast_fail_delete_server)
    }

    override fun onUpdatedTUServer(server: TellaUploadServer?) {
        val i = servers.indexOf(server)
        if (i != -1) {
            servers[i] = server
            listView!!.removeViewAt(i)
            listView!!.addView(getServerItem(server), i)
            //showToast(R.string.settings_docu_toast_server_updated)
        }
    }

    override fun onUpdateTUServerError(throwable: Throwable?) {
        //showToast(R.string.settings_docu_toast_fail_update_server)
    }

    override fun onServersLoaded(collectServers: List<CollectServer?>?) {
        Timber.d("++++++ onServersLoaded")
        Timber.d("++++++ collectServers(0) %s", collectServers?.get(0)?.name)
        listView!!.removeAllViews()
        servers.addAll(collectServers!!)
        createServerViews(servers)
    }

    override fun onLoadServersError(throwable: Throwable?) {
        //showToast(R.string.settings_docu_toast_fail_load_list_connected_servers)
    }

    override fun onCreatedServer(server: CollectServer?) {
        servers.add(server)
        listView!!.addView(getServerItem(server), servers.indexOf(server))
        //showToast(R.string.settings_docu_toast_server_created)
        if (MyApplication.isConnectedToInternet(requireContext())) {
            refreshPresenter!!.refreshBlankForms()
        }
    }

    override fun onCreateCollectServerError(throwable: Throwable?) {
        //showToast(R.string.settings_docu_toast_fail_create_server)
    }

    override fun onUpdatedServer(server: CollectServer?) {
        val i = servers.indexOf(server)
        if (i != -1) {
            servers[i] = server
            listView!!.removeViewAt(i)
            listView!!.addView(getServerItem(server), i)
            //showToast(R.string.settings_docu_toast_server_updated)
        }
    }

    override fun onUpdateServerError(throwable: Throwable?) {
       // showToast(R.string.settings_docu_toast_fail_update_server)
    }

    override fun onServersDeleted() {
        Preferences.setCollectServersLayout(false)
        servers.clear()
        listView!!.removeAllViews()
        turnOffAutoUpload()
        //setupCollectSettingsView()
        //showToast(R.string.settings_docu_toast_disconnect_servers_delete)
    }

    override fun onServersDeletedError(throwable: Throwable?) {}

    override fun onRemovedServer(server: CollectServer?) {
        servers.remove(server)
        listView!!.removeAllViews()
        createServerViews(servers)
        //showToast(R.string.settings_docu_toast_server_deleted)
    }

    override fun onRemoveServerError(throwable: Throwable?) {
        //showToast(R.string.settings_docu_toast_fail_delete_server)
    }

    override fun onRefreshBlankFormsError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    override fun getContext(): Context {
        return requireContext()
    }

    private fun getServerItem(server: Server?): View? {
        val inflater = LayoutInflater.from(context)
        @SuppressLint("InflateParams") val item =
            inflater.inflate(R.layout.servers_list_item, null) as LinearLayout
        val row = item.findViewById<ViewGroup>(R.id.server_row)
        val name = item.findViewById<TextView>(R.id.server_title)
        val options = item.findViewById<ImageView>(R.id.options)
        if (server != null) {
            name.text = server.name
            options.setOnClickListener { view: View? ->
                showDownloadedPopupMenu(
                    server,
                    row,
                    options
                )
            }
            /*name.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            getContext().getResources().getDrawable(
                    server.isChecked() ? R.drawable.ic_checked_green : R.drawable.watch_later_gray
            ),
            null);*/
        }
        item.tag = servers.indexOf(server)
        return item
    }

    private fun showDownloadedPopupMenu(server: Server, row: ViewGroup, options: ImageView) {
        val popup = PopupMenu(row.context, options)
        popup.inflate(R.menu.server_item_menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.edit_server -> if (server.serverType == ServerType.ODK_COLLECT) {
                    editCollectServer((server as CollectServer))
                } else {
                    editTUServer((server as TellaUploadServer))
                }
                R.id.remove_server -> if (server.serverType == ServerType.ODK_COLLECT) {
                    removeCollectServer((server as CollectServer))
                } else {
                    removeTUServer((server as TellaUploadServer))
                }
            }
            false
        }
        popup.show()
    }

    private fun removeCollectServer(server: CollectServer) {
        val dialog = DialogsUtil.showDialog(requireContext(),
                getString(R.string.settings_docu_delete_server_dialog_expl),
                getString(R.string.action_delete),
                getString(R.string.action_cancel),
                { dialog: DialogInterface, which: Int ->
                    collectServersPresenter!!.remove(server)
                    dialog.dismiss()
                }, null)
    }

    private fun removeTUServer(server: TellaUploadServer) {
        if (server.id == serversPresenter!!.autoUploadServerId) {
            val dialog = DialogsUtil.showDialog(requireContext(),
                    getString(R.string.settings_docu_delete_upload_server_dialog_expl),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    { dialog: DialogInterface, which: Int ->
                        tellaUploadServersPresenter!!.remove(server)
                        if (server.id == serversPresenter!!.autoUploadServerId) {
                            turnOffAutoUpload()
                        }
                        dialog.dismiss()
                    }, null)
        } else {
            val dialog = DialogsUtil.showDialog(requireContext(),
                    getString(R.string.settings_docu_delete_auto_server_dialog_expl),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    { dialog: DialogInterface, which: Int ->
                        tellaUploadServersPresenter!!.remove(server)
                        dialog.dismiss()
                    }, null)
        }
    }

    private fun turnOffAutoUpload() {
        //autoUploadSwitch.setChecked(false)
        serversPresenter!!.removeAutoUploadServersSettings()
       // autoUploadSettingsView.setVisibility(View.GONE)
    }

    private fun createRefreshPresenter() {
        if (refreshPresenter == null) {
            refreshPresenter = CollectBlankFormListRefreshPresenter(this)
        }
    }

    override fun onTellaUploadServerDialogCreate(server: TellaUploadServer?) {
        tellaUploadServersPresenter!!.create(server)
    }

    override fun onTellaUploadServerDialogUpdate(server: TellaUploadServer?) {
        tellaUploadServersPresenter!!.update(server)
    }

    override fun onCollectServerDialogCreate(server: CollectServer?) {
        collectServersPresenter!!.create(server)
    }

    override fun onCollectServerDialogUpdate(server: CollectServer?) {
        collectServersPresenter!!.update(server)
    }

    override fun onDialogDismiss() {
        TODO("Not yet implemented")
    }

    override fun showLoading() {}

    override fun hideLoading() {}

    override fun onTUServersLoaded(tellaUploadServers: List<TellaUploadServer?>) {
        listView!!.removeAllViews()
        servers.addAll(tellaUploadServers)
        createServerViews(servers)
        tuServers = tellaUploadServers as ArrayList<TellaUploadServer?>
        if (tuServers.size > 0) {
           // autoUploadSwitchView.setVisibility(View.VISIBLE)
           // setupAutoUploadSwitch()
           // setupAutoUploadView()
        } else {
           // autoUploadSwitchView.setVisibility(View.GONE)
        }
    }

    override fun onLoadTUServersError(throwable: Throwable?) {}

    override fun onCreatedTUServer(server: TellaUploadServer?) {
        servers.add(server)
        listView!!.addView(getServerItem(server), servers.indexOf(server))
        tuServers.add(server)
        if (tuServers.size == 1) {
            //autoUploadSwitchView.setVisibility(View.VISIBLE)
            //setupAutoUploadSwitch()
        }
        if (tuServers.size > 1) {
            //serverSelectLayout.setVisibility(View.VISIBLE)
        }
        //showToast(R.string.settings_docu_toast_server_created)
    }

    override fun onCreateTUServerError(throwable: Throwable?) {
       // showToast(R.string.settings_docu_toast_fail_create_server)
    }


    override fun onRemovedTUServer(server: TellaUploadServer?) {
        servers.remove(server)
        listView!!.removeAllViews()
        tuServers.remove(server)
        if (tuServers.size == 0) {
            //autoUploadSwitchView.setVisibility(View.GONE)
            //autoUploadSettingsView.setVisibility(View.GONE)
        }
        if (tuServers.size == 1) {
            //serverSelectLayout.setVisibility(View.GONE)
        }
        createServerViews(servers)
        //showToast(R.string.settings_docu_toast_server_deleted)
    }

    private fun createServerViews(servers: ArrayList<Server?>) {
        Timber.d("+++++ createServerViews for %d server(s)", servers.size)
        for (server in servers) {
            val view = getServerItem(server)
            listView!!.addView(view, servers.indexOf(server))
        }
    }

    private fun editCollectServer(server: CollectServer) {
        showCollectServerDialog(server)
    }

    private fun editTUServer(server: TellaUploadServer) {
        showTellaUploadServerDialog(server)
    }
}