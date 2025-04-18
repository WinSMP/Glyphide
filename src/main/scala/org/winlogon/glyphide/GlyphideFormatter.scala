// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.{CommandArguments, CommandExecutor}
import dev.jorel.commandapi.{CommandAPI, CommandAPICommand}

class GlyphideFormatter extends JavaPlugin {
    override def onEnable(): Unit = {
        val pluginManager = Bukkit.getServer.getPluginManager()

        saveDefaultConfig()
        pluginManager.registerEvents(ChatListener(this), this)

        CommandAPICommand("ghreload")
            .withPermission("glyphide.admin")
            .executesPlayer((player: Player, args: CommandArguments) => {
                reloadConfig()
                player.sendRichMessage(
                    "<gray>Glyphide configuration <dark_aqua>reloaded</dark_aqua>.</gray>"
                )
            })
            .register()
            getLogger.info("Glyphide has been enabled!")
    }

    override def onDisable(): Unit = {
        getLogger.info("Glyphide has been disabled!")
    }
}
