package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.R
import com.cherret.zaprett.utils.download
import com.cherret.zaprett.utils.getFileSha256
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getZaprettPath
import com.cherret.zaprett.utils.registerDownloadListenerHost
import com.cherret.zaprett.utils.restartService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseRepoViewModel(application: Application) : AndroidViewModel(application) {
    val context = application.applicationContext
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var hostLists = mutableStateOf<List<RepoItemInfo>>(emptyList())
        protected set

    var isRefreshing = mutableStateOf(false)
        protected set

    val isUpdate = mutableStateMapOf<String, Boolean>()
    val isInstalling = mutableStateMapOf<String, Boolean>()
    val isUpdateInstalling = mutableStateMapOf<String, Boolean>()

    abstract fun getInstalledLists(): Array<String>
    abstract fun getRepoList(callback: (List<RepoItemInfo>?) -> Unit)

    fun refresh() {
        isRefreshing.value = true
        getRepoList { list ->
            viewModelScope.launch(Dispatchers.IO) {
                val safeList = list ?: emptyList()
                val useModule = sharedPreferences.getBoolean("use_module", false)
                val listType = getHostListMode(sharedPreferences)
                val filteredList = safeList.filter { item ->
                    when (item.type) {
                        "list" -> listType == "whitelist"
                        "exclude-list" -> listType == "blacklist"
                        "nfqws" -> useModule
                        "byedpi" -> !useModule
                        else -> false
                    }
                }
                hostLists.value = filteredList
                isUpdate.clear()
                val existingHashes = getInstalledLists().map { getFileSha256(File(it)) }
                for (item in filteredList) {
                    isUpdate[item.name] = item.hash !in existingHashes
                }
                isRefreshing.value = false
            }
        }
    }

    fun isItemInstalled(item: RepoItemInfo): Boolean {
        return getInstalledLists().any { File(it).name == item.name }
    }

    fun install(item: RepoItemInfo) {
        isInstalling[item.name] = true
        val downloadId = download(context, item.url)
        registerDownloadListenerHost(context, downloadId) { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                val sourceFile = File(uri.path!!)
                val targetDir = when (item.type) {
                    "byedpi" -> File(getZaprettPath(), "strategies/byedpi")
                    "nfqws" -> File(getZaprettPath(), "strategies/nfqws")
                    else -> File(getZaprettPath(), "lists")
                }
                val targetFile = File(targetDir, uri.lastPathSegment!!)
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                isInstalling[item.name] = false
                isUpdate[item.name] = false
                refresh()
            }
        }
    }
    fun update(item: RepoItemInfo) {
        isUpdateInstalling[item.name] = true
        val downloadId = download(context, item.url)
        registerDownloadListenerHost(context, downloadId) { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                val sourceFile = File(uri.path!!)
                val targetDir = when (item.type) {
                    "byedpi" -> File(getZaprettPath(), "strategies/byedpi")
                    "nfqws" -> File(getZaprettPath(), "strategies/nfqws")
                    else -> File(getZaprettPath(), "lists")
                }
                val targetFile = File(targetDir, uri.lastPathSegment!!)
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                isUpdateInstalling[item.name] = false
                isUpdate[item.name] = false
                refresh()
            }
        }
    }

    fun showRestartSnackbar(snackbarHostState: SnackbarHostState) {
        viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                context.getString(R.string.pls_restart_snack),
                actionLabel = context.getString(R.string.btn_restart_service)
            )
            if (result == SnackbarResult.ActionPerformed) {
                restartService {}
                snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
            }
        }
    }
}