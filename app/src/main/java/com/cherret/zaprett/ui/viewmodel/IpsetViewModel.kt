package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.utils.disableIpset
import com.cherret.zaprett.utils.disableList
import com.cherret.zaprett.utils.enableIpset
import com.cherret.zaprett.utils.enableList
import com.cherret.zaprett.utils.getActiveExcludeIpsets
import com.cherret.zaprett.utils.getActiveExcludeLists
import com.cherret.zaprett.utils.getAllExcludeIpsets
import com.cherret.zaprett.utils.getAllIpsets
import com.cherret.zaprett.utils.getHostListMode
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.setHostListMode
import kotlinx.coroutines.CoroutineScope
import java.io.File

class IpsetViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun loadAllItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == "whitelist") getAllIpsets()
        else getAllExcludeIpsets()
    override fun loadActiveItems(): Array<String> =
        if (getHostListMode(sharedPreferences) == "whitelist") getActiveExcludeIpsets(sharedPreferences)
        else getActiveExcludeLists(sharedPreferences)

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableList(item, sharedPreferences)
        val success = File(item).delete()
        if (success) refresh()
        if (sharedPreferences.getBoolean("use_module", false)) {
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
        if (sharedPreferences.getBoolean("use_module", false)) {
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
    fun setListType(type : String) {
        setHostListMode(sharedPreferences, type)
        refresh()
    }

}