package top.wkbin.verydark

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickSettingService : TileService() {

    private val isRoot by lazy { RootChecker.isDeviceRooted() }

    private var isDark = false

    override fun onClick() {
        super.onClick()
        if (isRoot) {
            isDark = !isDark
            SettingsUtils.setReduceBrightColorsActivated(isDark)
            qsTile.state = if (isDark) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (isRoot) {
            isDark = SettingsUtils.getReduceBrightColorsActivated()
            qsTile.state = if (isDark) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }
}