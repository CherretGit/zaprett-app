package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.disableStrategy
import com.cherret.zaprett.enableStrategy
import com.cherret.zaprett.getActiveStrategies
import com.cherret.zaprett.getAllStrategies
import com.cherret.zaprett.getStatus
import kotlinx.coroutines.CoroutineScope
import java.io.File

class StrategyViewModel(application: Application): BaseListsViewModel(application) {
    override fun loadAllItems(): Array<String> = getAllStrategies()
    override fun loadActiveItems(): Array<String> = getActiveStrategies()

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableStrategy(item)
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
        if (isChecked) {
            checked.keys.forEach { key ->
                checked[key] = false
                disableStrategy(key)
            }
            checked[item] = true
            enableStrategy(item)
        }
        else {
            checked[item] = false
            disableStrategy(item)
        }
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