package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.utils.disableList
import com.cherret.zaprett.utils.enableList
import com.cherret.zaprett.utils.getActiveExcludeLists
import com.cherret.zaprett.utils.getActiveLists
import com.cherret.zaprett.utils.getAllExcludeLists
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.setHostListMode
import kotlinx.coroutines.CoroutineScope
import java.io.File

class HostsViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun loadAllItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getAllLists()
        else getAllExcludeLists()
    override fun loadActiveItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getActiveLists(sharedPreferences)
        else getActiveExcludeLists(sharedPreferences)

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableList(item, sharedPreferences)
        val success = File(item).delete()
        if (success) refresh()
        if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
            getStatus { isEnabled ->
                if (isEnabled && wasChecked) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    showRestartSnackbar(context, snackbarHostState, scope)
                }
            }
        }
    }

    override fun onCheckedChange(item: String, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) enableList(item, sharedPreferences) else disableList(item, sharedPreferences)
        if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
            getStatus { isEnabled ->
                if (isEnabled) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    showRestartSnackbar(
                        context,
                        snackbarHostState,
                        scope
                    )
                }
            }
        }
    }
    fun setListType(type : ListType) {
        setHostListMode(sharedPreferences, type)
        refresh()
    }

}