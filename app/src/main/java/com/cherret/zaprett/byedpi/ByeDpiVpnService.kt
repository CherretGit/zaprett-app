package com.cherret.zaprett.byedpi

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cherret.zaprett.MainActivity
import com.cherret.zaprett.R
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.utils.getActiveExcludeLists
import com.cherret.zaprett.utils.getActiveLists
import com.cherret.zaprett.utils.getActiveStrategy
import com.cherret.zaprett.utils.getAppsListMode
import com.cherret.zaprett.utils.getHostListMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ByeDpiVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var sharedPreferences: SharedPreferences
    companion object {
        private const val CHANNEL_ID = "zaprett_vpn_channel"
        private const val NOTIFICATION_ID = 1
        var status: ServiceStatus = ServiceStatus.Disconnected
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return when (intent?.action) {
            "START_VPN" -> {
                startForeground(NOTIFICATION_ID, createNotification())
                setupProxy()
                START_STICKY
            }
            "STOP_VPN" -> {
                stopProxy()
                stopSelf()
                START_NOT_STICKY
            }
            else -> {
                START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_zaprett_proxy),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_zaprett_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_zaprett_proxy))
            .setContentText(getString(R.string.notification_zaprett_description))
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentIntent(pendingIntent)
            .addAction(0, getString(R.string.btn_stop_service),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, ByeDpiVpnService::class.java).setAction("STOP_VPN"),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

    private fun setupProxy() {
        try {
            startSocksProxy()
            startByeDpi()
            status = ServiceStatus.Connected
        } catch (e: Exception) {
            Log.e("proxy", "Failed to start")
            status = ServiceStatus.Failed
            stopSelf()
        }
    }

    private fun startSocksProxy() {
        val dns = sharedPreferences.getString("dns", "8.8.8.8")?: "8.8.8.8"
        val ipv6 = sharedPreferences.getBoolean("ipv6", false)
        val socksIp = sharedPreferences.getString("ip", "127.0.0.1")?: "127.0.0.1"
        val socksPort = sharedPreferences.getString("port", "1080")?: "1080"
        val builder = Builder()
        builder.setSession(getString(R.string.notification_zaprett_proxy))
            .setMtu(1500)
            .addAddress("10.10.10.10", 32)
            .addDnsServer(dns)
            .addRoute("0.0.0.0", 0)
            .setMetered(false)
        if (ipv6) {
            builder.addAddress("fd00::1", 128)
                .addRoute("::", 0)
        }
        val appList = getAppsListMode(sharedPreferences)
        when (appList) {
            "blacklist" -> {
                builder.addDisallowedApplication(applicationContext.packageName)
                val blacklist = sharedPreferences.getStringSet("blacklist", emptySet())?: emptySet()
                blacklist.forEach {
                    builder.addDisallowedApplication(it)
                }
            }
            "whitelist" -> {
                val whitelist = sharedPreferences.getStringSet("whitelist", emptySet())?: emptySet()
                whitelist.forEach {
                    builder.addAllowedApplication(it)
                }
            }
            else -> {
                builder.addDisallowedApplication(applicationContext.packageName)
            }
        }
        Log.d("builder", builder.toString())
        vpnInterface = builder.establish()
        val tun2socksConfig = """
        | misc:
        |   task-stack-size: 81920
        | socks5:
        |   mtu: 8500
        |   address: $socksIp
        |   port: $socksPort
        |   udp: udp
        """.trimMargin("| ")
        val configPath = File.createTempFile("config", "tmp", cacheDir).apply {
            writeText(tun2socksConfig)
            deleteOnExit()
        }
        vpnInterface?.fd?.let { fd ->
            TProxyService.TProxyStartService(configPath.absolutePath, fd)
        }
    }

    private fun stopProxy() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            NativeBridge().stopProxy()
            TProxyService.TProxyStopService()
            status = ServiceStatus.Disconnected
        } catch (e: Exception) {
            Log.e("proxy", "error stop proxy")
        }
    }

    private fun startByeDpi() {
        val socksIp = sharedPreferences.getString("ip", "127.0.0.1")?: "127.0.0.1"
        val socksPort = sharedPreferences.getString("port", "1080")?: "1080"
        val listSet = if (getHostListMode(sharedPreferences) == "whitelist") getActiveLists(sharedPreferences) else getActiveExcludeLists(sharedPreferences)
        CoroutineScope(Dispatchers.IO).launch {
            val args = parseArgs(socksIp, socksPort, getActiveStrategy(sharedPreferences), prepareList(listSet), sharedPreferences)
            val result = NativeBridge().startProxy(args)
            if (result < 0) {
                Log.d("proxy","Failed to start byedpi proxy")
            } else {
                Log.d("proxy", "Byedpi proxy started successfully")
            }
        }
    }
    private suspend fun prepareList(actlists: Array<String>): String {
        if (actlists.isNotEmpty()) {
            val lists: Array<File> = actlists.map { File(it) }.toTypedArray()
            val hostlist = withContext(Dispatchers.IO) {
                File.createTempFile("hostlist", ".txt", cacheDir)
            }.apply { deleteOnExit() }
            withContext(Dispatchers.IO) {
                hostlist.printWriter().use { out ->
                    lists.forEach {
                        it.bufferedReader().useLines {
                            it.forEach {
                                out.println(it)
                            }
                        }
                    }
                }
            }
            return hostlist.absolutePath
        }
        return ""
    }

    private fun parseArgs(ip: String, port: String, rawArgs: List<String>, list : String, sharedPreferences: SharedPreferences): Array<String> {
        val regex = Regex("""--?\S+(?:=(?:[^"'\s]+|"[^"]*"|'[^']*'))?|[^\s]+""")
        val parsedArgs = rawArgs
            .flatMap { args -> regex.findAll(args).map { it.value } }
            .flatMap { arg ->
                if (getHostListMode(sharedPreferences) == "whitelist") {
                    when {
                        arg == "\$hostlist" && list.isNotEmpty() -> listOf("-H", list)
                        arg == "\$hostlist" && list.isEmpty() -> emptyList()
                        else -> listOf(arg)
                    }
                }
                else {
                    listOf("-H", list, "-An", arg).filter { it != "\$hostlist" }
                }
            }
            .toMutableList()
        return arrayOf("ciadpi", "--ip", ip, "--port", port) + parsedArgs
    }
}