package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cherret.zaprett.R
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.StrategyCheckResult
import com.cherret.zaprett.utils.disableStrategy
import com.cherret.zaprett.utils.enableStrategy
import com.cherret.zaprett.utils.getActiveLists
import com.cherret.zaprett.utils.getActiveStrategy
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getAllStrategies
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.startService
import com.cherret.zaprett.utils.stopService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class StrategySelectionViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = application.getSharedPreferences("settings", MODE_PRIVATE)
    val client = OkHttpClient.Builder()
        .callTimeout(prefs.getLong("probe_timeout", 1000L), TimeUnit.MILLISECONDS)
        .build()
    val context = application

    val strategyStates = mutableStateListOf<StrategyCheckResult>()

    init {
        loadStrategies()
    }

    fun loadStrategies() {
        val strategyList = getAllStrategies(prefs)
        strategyStates.clear()
        strategyList.forEach { name ->
            strategyStates += StrategyCheckResult(
                path = name,
                status = R.string.strategy_status_waiting,
                progress = 0f
            )
        }
    }

    suspend fun testDomain(domain : String) : Boolean  = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://${domain}")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful || (response.code in 300..399)
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun countReachable(urls: List<String>): Float = coroutineScope {
        if (urls.isEmpty()) return@coroutineScope 0f
        val results: List<Boolean> = urls.map { url ->
            async { testDomain(url) }
        }.awaitAll()
        val successCount = results.count { it }
        (successCount.toFloat() / urls.size.toFloat()).coerceIn(0f, 1f)
    }

    suspend fun readActiveListsLines(): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()

        getActiveLists(prefs).forEach { path ->
            runCatching {
                File(path).useLines { lines ->
                    lines.forEach { line ->
                        result += line
                    }
                }
            }.onFailure {
                Log.e("Error", "Occured error when creating list for check")
            }
        }
        result
    }
    suspend fun performTest() {
        val targets = readActiveListsLines()

        for (index in strategyStates.indices) {
            val current = strategyStates[index]
            strategyStates[index] = current.copy(status = R.string.strategy_status_testing)

            enableStrategy(current.path, prefs)

            if (prefs.getBoolean("use_module", false)) {
                getStatus { if (it) stopService {} }
                startService {}

                try {
                    val progress = countReachable(targets)

                    val old = strategyStates[index]
                    strategyStates[index] = old.copy(
                        progress = progress,
                        status = R.string.strategy_status_tested
                    )
                } finally {
                    stopService {}
                    disableStrategy(current.path, prefs)
                }
            }
            else {
                if (ByeDpiVpnService.status == ServiceStatus.Connected) {
                    context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                        action = "STOP_VPN"
                    })
                    delay(300L)
                }
                context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                    action = "START_VPN"
                })

                val connected = withTimeoutOrNull(10_000L) {
                    while (ByeDpiVpnService.status != ServiceStatus.Connected) {
                        delay(100L)
                    }
                    true
                } ?: false

                if (connected) delay(150L)
                try {
                    val progress = countReachable(targets)

                    val old = strategyStates[index]
                    strategyStates[index] = old.copy(
                        progress = progress,
                        status = R.string.strategy_status_tested
                    )
                } finally {
                    context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                        action = "STOP_VPN"
                    })
                    delay(200L)
                    disableStrategy(current.path, prefs)
                }
            }
        }

        val sorted = strategyStates.sortedByDescending { it.progress }
        strategyStates.clear()
        strategyStates.addAll(sorted)
    }
}