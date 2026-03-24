package com.cherret.zaprett.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cherret.zaprett.R
import com.cherret.zaprett.data.ItemType
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.RepoItemFull
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.data.ResolveResult
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.utils.DownloadUtils.download
import com.cherret.zaprett.utils.DownloadUtils.getFileSha256
import com.cherret.zaprett.utils.DownloadUtils.registerDownloadListener
import com.cherret.zaprett.utils.NetworkUtils.getRepo
import com.cherret.zaprett.utils.NetworkUtils.resolveDependencies
import com.cherret.zaprett.utils.checkStoragePermission
import com.cherret.zaprett.utils.getAllBin
import com.cherret.zaprett.utils.getAllByeDPIStrategies
import com.cherret.zaprett.utils.getAllExcludeIpsets
import com.cherret.zaprett.utils.getAllExcludeLists
import com.cherret.zaprett.utils.getAllIpsets
import com.cherret.zaprett.utils.getAllLibs
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getAllNfqws2Strategies
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getZaprettPath
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
import kotlinx.serialization.json.Json
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

    private val _items = MutableStateFlow<ResolveResult?>(null)
    val items: StateFlow<ResolveResult?> = _items

    var isRefreshing = mutableStateOf(false)
        protected set

    val isUpdate = mutableStateMapOf<String, Boolean>()
    val isInstalling = mutableStateMapOf<String, Boolean>()
    val isUpdateInstalling = mutableStateMapOf<String, Boolean>()

    abstract fun getInstalled(): Array<StorageData>
    abstract val repoTab: RepoTab
    val repoUrl = sharedPreferences.getString("repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-repo/refs/heads/main/index.json") ?: "https://raw.githubusercontent.com/CherretGit/zaprett-repo/refs/heads/main/index.json"
    private val Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun refresh() {
        viewModelScope.launch {
            val serviceType = getServiceType(sharedPreferences)
            val listType = getHostListMode(sharedPreferences)
            getRepo(repoUrl) { item ->
                when(repoTab) {
                    RepoTab.bins -> when(item.type) {
                        ItemType.bin -> true
                        else -> false
                    }
                    RepoTab.lua_libs -> when(item.type) {
                        ItemType.lua_lib -> true
                        else -> false
                    }
                    RepoTab.lists -> when (item.type) {
                        ItemType.list -> listType == ListType.whitelist
                        ItemType.list_exclude -> listType == ListType.blacklist
                        else -> false
                    }
                    RepoTab.ipsets -> when (item.type) {
                        ItemType.ipset -> listType == ListType.whitelist
                        ItemType.ipset_exclude -> listType == ListType.blacklist
                        else -> false
                    }
                    RepoTab.strategies -> when (item.type) {
                        ItemType.nfqws -> serviceType == ServiceType.nfqws
                        ItemType.nfqws2 -> serviceType == ServiceType.nfqws2
                        ItemType.byedpi -> serviceType == ServiceType.byedpi
                        else -> false
                    }
                }
            }
                .onStart { isRefreshing.value = true }
                .flatMapConcat { (index, list) ->
                    resolveDependencies(index, list)
                }
                .onEach { result ->
                    _items.value = result
                    isUpdate.clear()
                    isUpdate.putAll(
                        getInstalled().associate { item ->
                            val repoItem = result.roots.associateBy { it.manifest.id }[item.id]
                            item.id to (repoItem?.manifest?.version != item.version)
                        }
                    )
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

    fun isItemInstalled(item: RepoItemFull): Boolean {
        return getInstalled().any { it.id == item.manifest.id }
    }

    fun isDependencyInstalled(item: RepoItemFull): Boolean {
        val installed = when(item.index.type) {
            ItemType.bin -> getAllBin()
            ItemType.lua_lib -> getAllLibs()
            ItemType.byedpi -> getAllByeDPIStrategies()
            ItemType.nfqws -> getAllNfqwsStrategies()
            ItemType.nfqws2 -> getAllNfqws2Strategies()
            ItemType.list -> getAllLists()
            ItemType.list_exclude -> getAllExcludeLists()
            ItemType.ipset -> getAllIpsets()
            ItemType.ipset_exclude -> getAllExcludeIpsets()
        }
        return installed.any { it.id == item.manifest.id }
    }

    fun install(item: RepoItemFull) {
        val rootId = item.manifest.id
        val deps = _items.value?.dependencies
            ?.filter { rootId in it.dependencies }
            ?.map { it.manifest }
            .orEmpty()
            .filter { !isDependencyInstalled(it) }
        val download = listOf(item) + deps
        if (checkStoragePermission(context)) {
            download.forEach { item ->
                isInstalling[item.manifest.id] = true
                val downloadId = download(context, item.manifest.artifact.url)
                downloadAndProcess(item, context, downloadId)
            }
        }
        else _showPermissionDialog.value = true
    }

    fun hideNoPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun update(item: RepoItemFull) {
        val rootId = item.manifest.id
        val deps = _items.value?.dependencies
            ?.filter { rootId in it.dependencies }
            ?.map { it.manifest }
            .orEmpty()
            .filter { !isDependencyInstalled(it) }
        val download = listOf(item) + deps
        if (checkStoragePermission(context)) {
            download.forEach { item ->
                isUpdateInstalling[item.manifest.id] = true
                val downloadId = download(context, item.manifest.artifact.url)
                downloadAndProcess(item, context, downloadId)
            }
        }
        else _showPermissionDialog.value = true
    }

    fun downloadAndProcess(item: RepoItemFull, context: Context, downloadId: Long) {
        val index = item.index
        val item = item.manifest
        registerDownloadListener(context, downloadId, { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                val baseDir = getZaprettPath()
                val sourceFile = File(uri.path!!)

                if (getFileSha256(sourceFile) == item.artifact.sha256) {
                    val targetDirSuffix = when (index.type) {
                        ItemType.bin -> "bin"
                        ItemType.lua_lib -> "libs"
                        ItemType.byedpi -> "strategies/byedpi"
                        ItemType.nfqws -> "strategies/nfqws"
                        ItemType.nfqws2 -> "strategies/nfqws2"
                        ItemType.list -> "lists/include"
                        ItemType.list_exclude -> "lists/exclude"
                        ItemType.ipset -> "ipset/include"
                        ItemType.ipset_exclude -> "ipset/exclude"
                    }

                    val targetFile = baseDir
                        .resolve("files")
                        .resolve(targetDirSuffix)
                        .resolve(uri.lastPathSegment!!
                            .replace(Regex("""-\d+(?=\.|$)"""), ""))

                    targetFile.parentFile!!.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()

                    val manifestFile = baseDir.resolve("manifests").resolve(targetDirSuffix).resolve("${targetFile.name.substringBeforeLast(".")}.json")
                    manifestFile.parentFile!!.mkdirs()
                    manifestFile.writeText(
                        Json.encodeToString(
                            StorageData(
                                item.schema,
                                item.id,
                                item.name,
                                item.version,
                                item.author,
                                item.description,
                                item.dependencies,
                                file = targetFile.path
                            )
                        )
                    )

                    isInstalling[item.id] = false
                    isUpdateInstalling[item.id] = false
                    isUpdate[item.id] = false
                    refresh()
                }
                else {
                    isInstalling[item.id] = false
                    isUpdateInstalling[item.id] = false
                    _downloadErrorFlow.value = context.getString(R.string.error_hash_mismatch)
                    sourceFile.delete()
                    refresh()
                }
            }
        }, onError = {
            isInstalling[item.id] = false
            isUpdateInstalling[item.id] = false
            isUpdate[item.id] = false
            refresh()
            _downloadErrorFlow.value = it
        })
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