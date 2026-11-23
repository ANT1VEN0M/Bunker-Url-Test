package com.bunker.urltest

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class UrlTestTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("monitor", Context.MODE_PRIVATE)
        val active = prefs.getBoolean("active", false)
        if (active) {
            UrlTestMonitorService.stop(this)
            prefs.edit().putBoolean("active", false).apply()
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = getString(R.string.tile_label)
                updateTile()
            }
        } else {
            UrlTestMonitorService.start(this)
            prefs.edit().putBoolean("active", true).apply()
            qsTile?.apply {
                state = Tile.STATE_ACTIVE
                label = getString(R.string.tile_active)
                updateTile()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val active = getSharedPreferences("monitor", Context.MODE_PRIVATE).getBoolean("active", false)
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (active) getString(R.string.tile_active) else getString(R.string.tile_label)
            updateTile()
        }
    }
}
