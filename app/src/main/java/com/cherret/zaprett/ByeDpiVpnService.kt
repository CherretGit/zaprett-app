package com.cherret.zaprett

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return when (intent?.action) {
            "START_VPN" -> {
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
        if (getActiveStrategy(sharedPreferences).isNotEmpty()) {
            startForeground(NOTIFICATION_ID, createNotification())
            try {
                startSocksProxy()
                startByeDpi()
                status = ServiceStatus.Connected
            } catch (e: Exception) {
                Log.e("proxy", "Failed to start")
                status = ServiceStatus.Failed
            }
        }
        else {
            Toast.makeText(
                this@ByeDpiVpnService,
                getString(R.string.toast_no_strategy_selected),
                Toast.LENGTH_SHORT
            ).show()
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
            .addDisallowedApplication(applicationContext.packageName)
        if (ipv6) {
            builder.addAddress("fd00::1", 128)
                .addRoute("::", 0)
        }
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
        val listSet = sharedPreferences.getStringSet("lists", emptySet())?: emptySet()
        CoroutineScope(Dispatchers.IO).launch {
            val args = parseArgs(socksIp, socksPort, getActiveStrategy(sharedPreferences), listSet)
            val result = NativeBridge().startProxy(args)
            if (result < 0) {
                println("Failed to start byedpi proxy")
            } else {
                println("Byedpi proxy started successfully")
            }
        }
    }

    fun parseArgs(ip: String, port: String, rawArgs: List<String>, listSet: Set<String>): Array<String> {
        val regex = Regex("""--?\S+(?:=(?:[^"'\s]+|"[^"]*"|'[^']*'))?|[^\s]+""")
        val parsedArgs = rawArgs
            .flatMap { args -> regex.findAll(args).map { it.value } }
            .toMutableList()
        if (listSet.isNotEmpty()) {
            for (path in listSet) {
                parsedArgs.add("--hosts")
                parsedArgs.add(path)
            }
        }
        return arrayOf("ciadpi", "--ip", ip, "--port", port) + parsedArgs
    }
}