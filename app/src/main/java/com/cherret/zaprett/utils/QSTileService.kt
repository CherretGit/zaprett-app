package com.cherret.zaprett.utils

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cherret.zaprett.R

class QSTileService: TileService() {
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
            qsTile.subtitle = getString(R.string.qs_starting)
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
            startService {}
        }
        else {
            qsTile.subtitle = getString(R.string.qs_stopping)
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
            stopService {}
        }
        updateStatus()
    }

    private fun updateStatus() {
        if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("use_module", false)) {
            getStatus {
                if (it) {
                    qsTile.label = getString(R.string.qs_name)
                    qsTile.subtitle = getString(R.string.qs_working)
                    qsTile.state = Tile.STATE_ACTIVE
                    qsTile.updateTile()
                } else {
                    qsTile.label = getString(R.string.qs_name)
                    qsTile.subtitle = getString(R.string.qs_not_working)
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }
            }
        }
        else {
            qsTile.label = getString(R.string.qs_name)
            qsTile.subtitle = getString(R.string.qs_not_available)
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
        }
    }
}