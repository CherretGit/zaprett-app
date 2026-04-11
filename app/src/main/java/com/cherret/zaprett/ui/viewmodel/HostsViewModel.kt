package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.data.StorageData
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
    override fun loadAllItems(): Array<StorageData> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getAllLists()
        else getAllExcludeLists()
    override fun loadActiveItems(): Array<StorageData> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getActiveLists(sharedPreferences)
        else getActiveExcludeLists(sharedPreferences)

    override fun deleteItem(item: StorageData, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableList(item.manifestPath, sharedPreferences)
        val successArtifact = File(item.file).delete()
        val successManifest = File(item.manifestPath).delete()
        if (successArtifact && successManifest) refresh()
        if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
            getStatus { isEnabled ->
                if (isEnabled && wasChecked) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    showRestartSnackbar(context, snackbarHostState, scope)
                }
            }
        }
        refresh()
    }

    override fun onCheckedChange(item: StorageData, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) enableList(item.manifestPath, sharedPreferences) else disableList(item.manifestPath, sharedPreferences)
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