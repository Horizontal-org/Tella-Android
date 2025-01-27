package org.horizontal.tella.mobile.views.dialog.nextcloud

interface INextCloudAuthFlow {
    fun onStartRefreshLogin(serverUrl: String, userName: String, password: String)
    fun onStartCreateRemoteFolder(folderName: String)
}