// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class GlyphideFormatter extends JavaPlugin {
    override def onEnable(): Unit = {
        saveDefaultConfig()
        Bukkit.getServer.getPluginManager.registerEvents(ChatListener(this), this)

        registerCommand("ghreload", ReloadCommand(this))

        getLogger.info("Glyphide has been enabled!")
    }
}
