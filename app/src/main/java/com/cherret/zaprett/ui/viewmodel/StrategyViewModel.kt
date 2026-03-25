package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.utils.disableStrategy
import com.cherret.zaprett.utils.enableStrategy
import com.cherret.zaprett.utils.getActiveByeDPIStrategy
import com.cherret.zaprett.utils.getActiveNfqws2Strategy
import com.cherret.zaprett.utils.getActiveNfqwsStrategy
import com.cherret.zaprett.utils.getAllByeDPIStrategies
import com.cherret.zaprett.utils.getAllNfqws2Strategies
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getStatus
import kotlinx.coroutines.CoroutineScope
import java.io.File
import kotlin.emptyArray

class StrategyViewModel(application: Application): BaseListsViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val strategyProvider: StrategyProvider
        get() = when(getServiceType(sharedPreferences)) {
            ServiceType.nfqws -> NfqwsStrategyProvider()
            ServiceType.nfqws2 -> Nfqws2StrategyProvider()
            ServiceType.byedpi -> ByeDPIStrategyProvider(sharedPreferences)
        }
    override fun loadAllItems(): Array<StorageData> = strategyProvider.getAll()
    // костыль, желательно переделать
    override fun loadActiveItems(): Array<StorageData> {
        val activeItem = strategyProvider.getActive().getOrNull()
        return if (activeItem != null) arrayOf(activeItem)
        else emptyArray()
    }

    override fun deleteItem(item: StorageData, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        val wasChecked = checked[item] == true
        disableStrategy(item.manifestPath, sharedPreferences)
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
        else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected && wasChecked) {
                showRestartSnackbar(context, snackbarHostState, scope)
            }
        }
        refresh()
    }

    override fun onCheckedChange(item: StorageData, isChecked: Boolean, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        checked[item] = isChecked
        if (isChecked) {
            checked.keys.forEach { key ->
                checked[key] = false
                disableStrategy(key.manifestPath, sharedPreferences)
            }
            checked[item] = true
            enableStrategy(item.manifestPath, sharedPreferences)
        }
        else {
            checked[item] = false
            disableStrategy(item.manifestPath, sharedPreferences)
        }
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
}

interface StrategyProvider {
    fun getAll(): Array<StorageData>
    fun getActive(): Result<StorageData>
}

class NfqwsStrategyProvider : StrategyProvider {
    override fun getAll() = getAllNfqwsStrategies()
    override fun getActive() = getActiveNfqwsStrategy()
}

class Nfqws2StrategyProvider : StrategyProvider {
    override fun getAll() = getAllNfqws2Strategies()
    override fun getActive() = getActiveNfqws2Strategy()
}

class ByeDPIStrategyProvider(private val sharedPreferences: SharedPreferences) : StrategyProvider {
    override fun getAll() = getAllByeDPIStrategies()
    override fun getActive() = getActiveByeDPIStrategy(sharedPreferences)
}