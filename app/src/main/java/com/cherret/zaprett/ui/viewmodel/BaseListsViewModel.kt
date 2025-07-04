package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.R
import com.cherret.zaprett.utils.getZaprettPath
import com.cherret.zaprett.utils.restartService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class BaseListsViewModel(application: Application) : AndroidViewModel(application) {
    val context = application
    var allItems by mutableStateOf<List<String>>(emptyList())
        private set
    var activeItems by mutableStateOf<List<String>>(emptyList())
        private set
    val checked = mutableStateMapOf<String, Boolean>()
    var isRefreshing by mutableStateOf(false)
        private set

    abstract fun loadAllItems(): Array<String>
    abstract fun loadActiveItems(): Array<String>
    abstract fun onCheckedChange(item: String, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope)
    abstract fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope)

    fun refresh() {
        isRefreshing = true
        allItems = loadAllItems().toList()
        activeItems = loadActiveItems().toList()
        checked.clear()
        allItems.forEach { list ->
            checked[list] = activeItems.contains(list)
        }
        isRefreshing = false
    }
    
    fun showRestartSnackbar(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        scope.launch {
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

    fun copySelectedFile(context: Context, path: String, uri: Uri) {
        if (!Environment.isExternalStorageManager()) return
        val contentResolver = context.contentResolver
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else "copied_file"
        } ?: "copied_file"

        val directory = File(getZaprettPath() + path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val outputFile = File(getZaprettPath() + path, fileName)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            refresh()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}