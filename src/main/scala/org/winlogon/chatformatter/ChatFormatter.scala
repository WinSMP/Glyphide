package org.winlogon.chatformatter

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor

class ChatFormatter extends JavaPlugin {
  override def onEnable(): Unit = {
    val pluginManager = Bukkit.getServer.getPluginManager()

    if (!pluginManager.isPluginEnabled("LuckPerms")) {
      getLogger.severe("LuckPerms is required for formatting the chat.")
      getLogger.severe("Please download LuckPerms and try again.")
      pluginManager.disablePlugin(this)
      return
    }

    saveDefaultConfig()
    pluginManager.registerEvents(new ChatListener(this), this)

    new CommandAPICommand("chatformatter")
      .withAliases("cf")
      .withArguments(new StringArgument("subcommand"))
      .withSubcommand(
        new CommandAPICommand("reload")
          .withPermission("chatformatter.admin")
          .executesPlayer((player: Player, args: CommandArguments) => {
              reloadConfig()
              player.sendMessage("§7ChatFormatter configuration §3reloaded§7.")
          })
      )
      .register()

    getLogger.info("ChatFormatter has been enabled!")
  }

  override def onDisable(): Unit = {
    getLogger.info("ChatFormatter has been disabled!")
  }
}
