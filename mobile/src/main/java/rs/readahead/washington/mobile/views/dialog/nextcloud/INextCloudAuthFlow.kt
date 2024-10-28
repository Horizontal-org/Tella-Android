package rs.readahead.washington.mobile.views.dialog.nextcloud

interface INextCloudAuthFlow {
    fun onStartRefreshLogin(serverUrl: String, userName: String, password: String)
}