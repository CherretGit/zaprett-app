package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.utils.disableIpset
import com.cherret.zaprett.utils.enableIpset
import com.cherret.zaprett.utils.getActiveExcludeIpsets
import com.cherret.zaprett.utils.getActiveIpsets
import com.cherret.zaprett.utils.getAllExcludeIpsets
import com.cherret.zaprett.utils.getAllIpsets
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.setHostListMode
import kotlinx.coroutines.CoroutineScope
import java.io.File

class IpsetViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun loadAllItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getAllIpsets()
        else getAllExcludeIpsets()
    override fun loadActiveItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getActiveIpsets(sharedPreferences)
        else getActiveExcludeIpsets(sharedPreferences)

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableIpset(item, sharedPreferences)
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
        if (isChecked) enableIpset(item, sharedPreferences) else disableIpset(item, sharedPreferences)
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