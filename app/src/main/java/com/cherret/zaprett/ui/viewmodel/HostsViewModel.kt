package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.utils.disableList
import com.cherret.zaprett.utils.enableList
import com.cherret.zaprett.utils.getActiveLists
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getStatus
import kotlinx.coroutines.CoroutineScope
import java.io.File

class HostsViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun loadAllItems(): Array<String> = getAllLists()
    override fun loadActiveItems(): Array<String> = getActiveLists(sharedPreferences)

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableList(item, sharedPreferences)
        val success = File(item).delete()
        if (success) refresh()
        if (sharedPreferences.getBoolean("use_module", false)) {
            getStatus { isEnabled ->
                if (isEnabled && wasChecked) {
                    showRestartSnackbar(context, snackbarHostState, scope)
                }
            }
        }
    }

    override fun onCheckedChange(item: String, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) enableList(item, sharedPreferences) else disableList(item, sharedPreferences)
        if (sharedPreferences.getBoolean("use_module", false)) {
            getStatus { isEnabled ->
                if (isEnabled) {
                    showRestartSnackbar(
                        context,
                        snackbarHostState,
                        scope
                    )
                }
            }
        }
    }
}