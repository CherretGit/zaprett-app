package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.R
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.StrategyCheckResult
import com.cherret.zaprett.data.StrategyTestingStatus
import com.cherret.zaprett.utils.disableStrategy
import com.cherret.zaprett.utils.enableStrategy
import com.cherret.zaprett.utils.getActiveLists
import com.cherret.zaprett.utils.getActiveStrategy
import com.cherret.zaprett.utils.getAllStrategies
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.startService
import com.cherret.zaprett.utils.stopService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class StrategySelectionViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = application.getSharedPreferences("settings", MODE_PRIVATE)
    val context = application
    private val _requestVpnPermission = MutableStateFlow(false)
    val requestVpnPermission = _requestVpnPermission.asStateFlow()
    val strategyStates = mutableStateListOf<StrategyCheckResult>()
    var noHostsCard = mutableStateOf(false)
        private set

    init {
        loadStrategies()
        checkHosts()
    }

    fun buildHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .callTimeout(prefs.getLong("probe_timeout", 1000L), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
        if (!prefs.getBoolean("use_module", false)) {
            val ip = prefs.getString("ip", "127.0.0.1") ?: "127.0.0.1"
            val port = prefs.getString("port", "1080")?.toIntOrNull() ?: 1080
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(ip, port))
            builder.proxy(proxy)
        }
        return builder.build()
    }

    fun loadStrategies() {
        val strategyList = getAllStrategies(prefs)
        strategyStates.clear()
        strategyList.forEach { name ->
            strategyStates += StrategyCheckResult(
                path = name,
                status = StrategyTestingStatus.Waiting,
                progress = 0f,
                domains = emptyList()
            )
        }
    }

    suspend fun testDomain(domain : String) : Boolean  = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://${domain}")
            .build()
        try {
            buildHttpClient().newCall(request).execute().use { response ->
                val body = response.body.byteStream().readBytes()
                val contentLength = response.body.contentLength()
                contentLength <= 0 || body.size.toLong() >= contentLength
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun countReachable(index: Int, urls: List<String>): Float = coroutineScope {
        if (urls.isEmpty()) return@coroutineScope 0f
        val results: List<String> = urls.map { url ->
            async { if (testDomain(url)) url else null }
        }.awaitAll().filterNotNull()
        strategyStates[index].domains = results
        (results.size.toFloat() / urls.size.toFloat()).coerceIn(0f, 1f)
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
            strategyStates[index] = current.copy(status = StrategyTestingStatus.Testing)
            enableStrategy(current.path, prefs)
            if (prefs.getBoolean("use_module", false)) {
                getStatus { if (it) stopService {} }
                startService {}
                try {
                    val progress = countReachable(index, targets)
                    val old = strategyStates[index]
                    strategyStates[index] = old.copy(
                        progress = progress,
                        status = StrategyTestingStatus.Completed
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
                _requestVpnPermission.value = true
                /*context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                    action = "START_VPN"
                })*/
                val connected = withTimeoutOrNull(10_000L) {
                    while (ByeDpiVpnService.status != ServiceStatus.Connected) {
                        delay(100L)
                    }
                    true
                } ?: false
                if (connected) delay(150L)
                try {
                    val progress = countReachable(index,targets)
                    val old = strategyStates[index]

                    strategyStates[index] = old.copy(
                        progress = progress,
                        status = StrategyTestingStatus.Completed
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

    fun checkHosts() {
        if (getActiveLists(prefs).isEmpty()) noHostsCard.value = true
        Log.d("getActiveLists.isEmpty", getActiveLists(prefs).isEmpty().toString())
    }
    fun startVpn() {
        ContextCompat.startForegroundService(context, Intent(context, ByeDpiVpnService::class.java).apply { action = "START_VPN" })
    }
    fun clearVpnPermissionRequest() {
        _requestVpnPermission.value = false
    }
}