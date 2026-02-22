package com.cherret.zaprett.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cherret.zaprett.R
import com.cherret.zaprett.data.DependencyEntry
import com.cherret.zaprett.data.DependencyUI
import com.cherret.zaprett.data.ItemType
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.RepoItemFull
import com.cherret.zaprett.data.RepoItemUI
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.utils.checkStoragePermission
import com.cherret.zaprett.utils.download
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getRepo
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getZaprettPath
import com.cherret.zaprett.utils.registerDownloadListener
import com.cherret.zaprett.utils.resolveDependencies
import com.cherret.zaprett.utils.restartService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseRepoViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    val context: Context = application.applicationContext
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _errorFlow = MutableStateFlow<Throwable?>(null)
    val errorFlow: StateFlow<Throwable?> = _errorFlow

    private val _downloadErrorFlow = MutableStateFlow<String?>(null)

    val downloadErrorFlow: StateFlow<String?> = _downloadErrorFlow

    private var _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog
    private val repoItems = mutableMapOf<String, RepoItemFull>()
    private val _dependencyItems = mutableStateListOf<DependencyEntry>()
    val dependencyItems: List<DependencyEntry> = _dependencyItems


    private val _items = mutableStateOf<List<RepoItemUI>>(emptyList())
    val items: List<RepoItemUI> get() = _items.value

    private var _dependencyList = MutableStateFlow<List<DependencyUI>>(emptyList())
    val dependencyList: StateFlow<List<DependencyUI>> = _dependencyList

    var isRefreshing = mutableStateOf(false)
        protected set

    val isUpdate = mutableStateMapOf<String, Boolean>()
    val isInstalling = mutableStateMapOf<String, Boolean>()
    val isUpdateInstalling = mutableStateMapOf<String, Boolean>()

    abstract fun getInstalledLists(): Array<String>
    abstract val repoTab: RepoTab
    fun getRepoList() = getRepo(sharedPreferences.getString("repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-repo/refs/heads/main/index.json") ?: "https://raw.githubusercontent.com/CherretGit/zaprett-repo/refs/heads/main/index.json")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun refresh() {
        viewModelScope.launch {
            getRepoList()
                .onStart { isRefreshing.value = true }
                .flatMapConcat { list ->
                    val serviceType = getServiceType(sharedPreferences)
                    val listType = getHostListMode(sharedPreferences)
                    val filteredList = list.filter { item ->
                        when(repoTab) {
                            RepoTab.lists -> when (item.index.type) {
                                ItemType.list -> listType == ListType.whitelist
                                ItemType.list_exclude -> listType == ListType.blacklist
                                else -> false
                            }
                            RepoTab.ipsets -> when (item.index.type) {
                                ItemType.ipset -> listType == ListType.whitelist
                                ItemType.ipset_exclude -> listType == ListType.blacklist
                                else -> false
                            }
                            RepoTab.strategies -> when (item.index.type) {
                                ItemType.nfqws -> serviceType == ServiceType.nfqws
                                ItemType.nfqws2 -> serviceType == ServiceType.nfqws2
                                ItemType.byedpi -> serviceType == ServiceType.byedpi
                                else -> false
                            }
                        }
                    }
                    resolveDependencies(filteredList.map { it })
                }
                .onEach { result ->
                    _dependencyItems.clear()
                    _dependencyItems.addAll(result.dependencies)
                    _items.value = result.roots.map { item ->
                        repoItems[item.manifest.name] = item
                        RepoItemUI(
                            name = item.manifest.name,
                            author = item.manifest.author,
                            description = item.manifest.description
                        )
                    }
                    _dependencyList.value = result.dependencies.map { item ->
                        DependencyUI(
                            name = item.manifest.name
                        )
                    }
                    dependencyItems
                    isUpdate.clear()
                }
                .catch { e -> _errorFlow.value = e }
                .onCompletion { isRefreshing.value = false }
                .collect()
        }
    }

    fun clearError() {
        _errorFlow.value = null
    }

    fun clearDownloadError() {
        _downloadErrorFlow.value = null
    }

    fun isItemInstalled(item: RepoItemUI): Boolean {
        return getInstalledLists().any { File(it).name == item.name }
    }

    fun install(item: RepoItemUI) {
        val index = repoItems[item.name]!!.index
        val item = repoItems[item.name]!!.manifest
        when (checkStoragePermission(context)) {
            true -> {
                isInstalling[item.name] = true
                val downloadId = download(context, item.artifact.url)
                registerDownloadListener(context, downloadId, { uri ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val sourceFile = File(uri.path!!)
                        val targetDir = when (index.type) {
                            ItemType.byedpi -> File(getZaprettPath(), "strategies/byedpi")
                            ItemType.nfqws -> File(getZaprettPath(), "strategies/nfqws")
                            ItemType.nfqws2 -> File(getZaprettPath(), "strategies/nfqws2")
                            ItemType.list -> File(getZaprettPath(), "lists/include")
                            ItemType.list_exclude -> File(getZaprettPath(), "lists/exclude")
                            ItemType.ipset -> File(getZaprettPath(), "ipset/include")
                            ItemType.ipset_exclude -> File(getZaprettPath(), "ipset/exclude")
                        }
                        val targetFile = File(targetDir, uri.lastPathSegment!!)
                        sourceFile.copyTo(targetFile, overwrite = true)
                        sourceFile.delete()
                        isInstalling[item.name] = false
                        isUpdate[item.name] = false
                        refresh()
                    }
                }, onError = {
                    isInstalling[item.name] = false
                    isUpdate[item.name] = false
                    refresh()
                    _downloadErrorFlow.value = it
                })
            }
            false -> _showPermissionDialog.value = true
        }
    }

    fun hideNoPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun update(item: RepoItemUI) {
        val index = repoItems[item.name]!!.index
        val item = repoItems[item.name]!!.manifest
        isUpdateInstalling[item.name] = true
        val downloadId = download(context, item.artifact.url)
        registerDownloadListener(
            context,
            downloadId,
            onDownloaded = { uri ->
                viewModelScope.launch(Dispatchers.IO) {
                    val sourceFile = File(uri.path!!)
                    val targetDir = when (index.type) {
                        ItemType.byedpi -> File(getZaprettPath(), "strategies/byedpi")
                        ItemType.nfqws -> File(getZaprettPath(), "strategies/nfqws")
                        ItemType.nfqws2 -> File(getZaprettPath(), "strategies/nfqws2")
                        ItemType.list -> File(getZaprettPath(), "lists/include")
                        ItemType.list_exclude -> File(getZaprettPath(), "lists/exclude")
                        ItemType.ipset -> File(getZaprettPath(), "ipset/include")
                        ItemType.ipset_exclude -> File(getZaprettPath(), "ipset/exclude")
                    }
                    val targetFile = File(targetDir, uri.lastPathSegment!!)
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()
                    isUpdateInstalling[item.name] = false
                    isUpdate[item.name] = false
                    refresh()
                }
            },
            onError = { error ->
                isUpdateInstalling[item.name] = false
                isUpdate[item.name] = false
                refresh()
                _downloadErrorFlow.value = error
            }
        )
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