package org.winlogon

import org.bukkit.Bukkit
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.plugin.java.JavaPlugin

class ChatFormatter extends JavaPlugin {
  val commands = ("chatformatter", "cf", "formatter").toList

  override def onEnable(): Unit = {
    val pluginManager = Bukkit.getServer.getPluginManager()

    if (!pluginManager.isPluginEnabled("LuckPerms")) {
      Bukkit.getLogger.severe("LuckPerms is required for formatting the chat.")
      Bukkit.getLogger.severe("Please download LuckPerms and try again.")
      pluginManager.disablePlugin(this)
      return
    }

    saveDefaultConfig()
    pluginManager.registerEvents(new ChatListener(this), this)

    for (command <- commands) {
      getCommand(command).setExecutor(new ChatFormatterCommand(this))
    }
    getLogger.info("ChatFormatter has been enabled!")
  }

  override def onDisable(): Unit = {
    getLogger.info("ChatFormatter has been disabled!")
  }
}
