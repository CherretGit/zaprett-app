package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.disableList
import com.cherret.zaprett.enableList
import com.cherret.zaprett.getActiveLists
import com.cherret.zaprett.getAllLists
import com.cherret.zaprett.getStatus
import kotlinx.coroutines.CoroutineScope
import java.io.File

class HostsViewModel(application: Application): BaseListsViewModel(application) {
    override fun loadAllItems(): Array<String> = getAllLists()
    override fun loadActiveItems(): Array<String> = getActiveLists()

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableList(item)
        val success = File(item).delete()
        if (success) refresh()
        getStatus { isEnabled ->
            if (isEnabled && wasChecked) {
                showRestartSnackbar(context, snackbarHostState, scope)
            }
        }
    }

    override fun onCheckedChange(item: String, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) enableList(item) else disableList(item)
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
