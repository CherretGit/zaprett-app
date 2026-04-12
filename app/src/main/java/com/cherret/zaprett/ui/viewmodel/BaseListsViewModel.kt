package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.R
import com.cherret.zaprett.data.ListUiItem
import com.cherret.zaprett.data.ListUiState
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.utils.checkStoragePermission
import com.cherret.zaprett.utils.getActiveStrategy
import com.cherret.zaprett.utils.restartService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class BaseListsViewModel(application: Application) : AndroidViewModel(application) {
    val context = application
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    protected val _listUiState = MutableStateFlow(ListUiState())
    val listUiState: StateFlow<ListUiState> = _listUiState.asStateFlow()
    private val _pendingFileName = MutableStateFlow<String?>(null)
    val pendingFileName: StateFlow<String?> = _pendingFileName.asStateFlow()
    private val _pendingFileUri = MutableStateFlow<Uri?>(null)
    val pendingFileUri: StateFlow<Uri?> = _pendingFileUri.asStateFlow()

    private var _showNoPermissionDialog = MutableStateFlow(false)
    val showNoPermissionDialog: StateFlow<Boolean> = _showNoPermissionDialog
    private var _showGenerateManifestDialog = MutableStateFlow(false)
    val showGenerateManifestDialog: StateFlow<Boolean> = _showGenerateManifestDialog

    abstract fun loadAllItems(): Array<StorageData>
    abstract fun loadActiveItems(): Array<StorageData>
    abstract fun onCheckedChange(item: ListUiItem, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope)
    abstract fun deleteItem(item: ListUiItem, snackbarHostState: SnackbarHostState, scope: CoroutineScope)

    fun refresh() {
        when (checkStoragePermission(context)) {
            true -> {
                _listUiState.value = _listUiState.value.copy(
                    isRefreshing = true
                )
                val allItems = loadAllItems().toList()
                val activeItems = loadActiveItems().toList()
                val strategy = getActiveStrategy(sharedPreferences).getOrNull()
                val items = allItems.map { item ->
                    ListUiItem(
                        data = item,
                        isChecked = item in activeItems,
                        isUsing = strategy?.dependencies?.contains(item.manifestPath) == true
                    )
                }
                _listUiState.value = _listUiState.value.copy(
                    items = items,
                    isRefreshing = false
                )
            }
            false -> _showNoPermissionDialog.value = true
        }
    }

    fun hideNoPermissionDialog() {
        _showNoPermissionDialog.value = false
    }

    fun showRestartSnackbar(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                context.getString(R.string.pls_restart_snack),
                actionLabel = context.getString(R.string.btn_restart_service)
            )
            if (result == SnackbarResult.ActionPerformed) {
                restartService { error ->
                    _listUiState.value = _listUiState.value.copy(
                        error = error
                    )
                }
                snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
            }
        }
    }

    fun clearError() {
        _listUiState.value = _listUiState.value.copy(
            error = null
        )
    }

    fun prepareImport(context: Context, path: File, uri: Uri) {
        val name = context.contentResolver.query(uri, null, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
        } ?: "copied_file"
        _pendingFileName.value = path.resolve(name).absolutePath
        _pendingFileUri.value = uri
        _showGenerateManifestDialog.value = true
    }

    fun cancelImport() {
        _pendingFileName.value = null
        _pendingFileUri.value = null
        _showGenerateManifestDialog.value = false
    }

    fun import(context: Context, manifestPath: File, manifest: StorageData) {
        val uri = _pendingFileUri.value ?: return
        val manifestFile = manifestPath.resolve("${manifest.id}.json")
        manifestFile.parentFile!!.mkdirs()
        manifestFile.writeText(json.encodeToString(manifest))
        copySelectedFile(context, manifest.file, uri)
        _pendingFileName.value = null
        _pendingFileUri.value = null
        _showGenerateManifestDialog.value = false
        refresh()
    }

    fun copySelectedFile(context: Context, path: String, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()) return
        }
        else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) return
        val contentResolver = context.contentResolver
        val directory = File(path)
        try {
            directory.parentFile?.mkdirs()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(directory).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}