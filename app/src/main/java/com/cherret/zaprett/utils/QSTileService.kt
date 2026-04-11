package com.cherret.zaprett.utils

import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.cherret.zaprett.R
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.ServiceType

class QSTileService: TileService() {
    private lateinit var prefs: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        prefs = applicationContext.getSharedPreferences("settings", MODE_PRIVATE)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateStatus()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateStatus()
    }

    override fun onClick() {
        super.onClick()
        if (qsTile.state == Tile.STATE_INACTIVE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                qsTile.subtitle = getString(R.string.qs_starting)
            }
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
            if (getServiceType(prefs) != ServiceType.byedpi){
                startService {}
            }
            else {
                val prepareIntent = VpnService.prepare(applicationContext)
                if (prepareIntent != null) {
                    prepareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(prepareIntent)
                } else {
                    ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, ByeDpiVpnService::class.java).apply { action = "START_VPN" })
                }
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                qsTile.subtitle = getString(R.string.qs_stopping)
            }
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
            if (getServiceType(prefs) != ServiceType.byedpi){
                stopService {}
            }
            else {
                applicationContext.startService(Intent(applicationContext, ByeDpiVpnService::class.java).apply { action = "STOP_VPN" })
            }
        }
        updateStatus()
    }

    private fun updateStatus() {
        if (getServiceType(prefs) != ServiceType.byedpi) {
            getStatus {
                if (it) {
                    qsTile.label = getString(R.string.qs_name)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        qsTile.subtitle = getString(R.string.qs_working)
                    }
                    qsTile.state = Tile.STATE_ACTIVE
                    qsTile.updateTile()
                } else {
                    qsTile.label = getString(R.string.qs_name)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        qsTile.subtitle = getString(R.string.qs_not_working)
                    }
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }
            }
        }
        else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected) {
                qsTile.label = getString(R.string.qs_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    qsTile.subtitle = getString(R.string.qs_working)
                }
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.updateTile()
            } else {
                qsTile.label = getString(R.string.qs_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    qsTile.subtitle = getString(R.string.qs_not_working)
                }
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
            }
        }
    }
}