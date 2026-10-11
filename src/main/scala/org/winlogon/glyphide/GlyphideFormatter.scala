// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class GlyphideFormatter extends JavaPlugin {
    override def onEnable(): Unit = {
        saveDefaultConfig()

        // one snapshot for the listener and the reload command, so /ghreload swaps both together
        val settings = SettingsHolder(this)

        Bukkit.getServer.getPluginManager.registerEvents(ChatListener(settings), this)
        Bukkit.getServer.getPluginManager.registerEvents(InventoryGuard(), this)

        registerCommand("ghreload", ReloadCommand(settings))

        getLogger.info("Glyphide has been enabled!")
    }
}
