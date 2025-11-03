package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.utils.disableStrategy
import com.cherret.zaprett.utils.enableStrategy
import com.cherret.zaprett.utils.getActiveByeDPIStrategy
import com.cherret.zaprett.utils.getActiveNfqwsStrategy
import com.cherret.zaprett.utils.getAllByeDPIStrategies
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getStatus
import kotlinx.coroutines.CoroutineScope
import java.io.File

class StrategyViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val strategyProvider: StrategyProvider
        get() = if (sharedPreferences.getBoolean("use_module", false)) {
            NfqwsStrategyProvider()
        } else {
            ByeDPIStrategyProvider(sharedPreferences)
        }

    override fun loadAllItems(): Array<String> = strategyProvider.getAll()
    override fun loadActiveItems(): Array<String> = strategyProvider.getActive()

    override fun deleteItem(item: String, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableStrategy(item, sharedPreferences)
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
        else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected && wasChecked) {
                showRestartSnackbar(context, snackbarHostState, scope)
            }
        }
    }

    override fun onCheckedChange(item: String, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) {
            checked.keys.forEach { key ->
                checked[key] = false
                disableStrategy(key, sharedPreferences)
            }
            checked[item] = true
            enableStrategy(item, sharedPreferences)
        }
        else {
            checked[item] = false
            disableStrategy(item, sharedPreferences)
        }
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
}

interface StrategyProvider {
    fun getAll(): Array<String>
    fun getActive(): Array<String>
}

class NfqwsStrategyProvider : StrategyProvider {
    override fun getAll() = getAllNfqwsStrategies()
    override fun getActive() = getActiveNfqwsStrategy()
}

class ByeDPIStrategyProvider(private val sharedPreferences: SharedPreferences) : StrategyProvider {
    override fun getAll() = getAllByeDPIStrategies()
    override fun getActive() = getActiveByeDPIStrategy(sharedPreferences)
}