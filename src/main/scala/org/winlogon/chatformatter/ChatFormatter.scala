package org.winlogon.chatformatter

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.{CommandArguments, CommandExecutor}
import dev.jorel.commandapi.{CommandAPI, CommandAPICommand}

class ChatFormatter extends JavaPlugin {
    override def onEnable(): Unit = {
        val pluginManager = Bukkit.getServer.getPluginManager()

        saveDefaultConfig()
        pluginManager.registerEvents(ChatListener(this), this)

        CommandAPICommand("cfreload")
            .withPermission("chatformatter.admin")
            .executesPlayer((player: Player, args: CommandArguments) => {
                reloadConfig()
                player.sendRichMessage(
                    "<gray>ChatFormatter configuration <dark_aqua>reloaded</dark_aqua>.</gray>"
                )
            })
            .register()
            getLogger.info("ChatFormatter has been enabled!")
    }

    override def onDisable(): Unit = {
        getLogger.info("ChatFormatter has been disabled!")
    }
}
