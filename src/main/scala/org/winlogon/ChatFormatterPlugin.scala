package org.winlogon

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin
import net.luckperms.api.{LuckPerms, LuckPermsProvider}
import net.luckperms.api.model.user.User

class ChatFormatterPlugin extends JavaPlugin {

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
    getCommand("chatformatter").setExecutor(new ChatCommand(this))
    getLogger.info("ChatFormatterPlugin has been enabled!")
  }

  override def onDisable(): Unit = {
    getLogger.info("ChatFormatterPlugin has been disabled!")
  }

  def getPrefix(player: Player): String = {
    Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
      .flatMap(user => Option(user.getCachedData.getMetaData.getPrefix))
      .map(prefix => ChatColor.translateAlternateColorCodes('&', prefix))
      .getOrElse("")
  }

  def getSuffix(player: Player): String = {
    Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
      .flatMap(user => Option(user.getCachedData.getMetaData.getSuffix))
      .map(suffix => ChatColor.translateAlternateColorCodes('&', suffix))
      .getOrElse("")
  }

  def formatMessage(player: Player, message: String): String = {
    val permissions = Map(
      "chatformatter.color" -> "(?i)&[0-9a-f]",
      "chatformatter.bold" -> "(?i)&l",
      "chatformatter.italic" -> "(?i)&o",
      "chatformatter.underline" -> "(?i)&n",
      "chatformatter.magic" -> "(?i)&k"
    )

    permissions.foldLeft(ChatColor.translateAlternateColorCodes('&', message)) {
      case (msg, (perm, regex)) =>
        if (!player.hasPermission(perm)) msg.replaceAll(regex, "") else msg
    }
  }
}

class ChatListener(plugin: ChatFormatterPlugin) extends Listener {

  @EventHandler
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    val player: Player = event.getPlayer
    val message = event.message().toString
  
    val prefix = plugin.getPrefix(player)
    val suffix = plugin.getSuffix(player)
    val formattedMessage = plugin.formatMessage(player, message)
  
    val chatFormat = plugin.getConfig.getString("chat.format", "<$prefix${player.getName}$suffix> $message")
      .replace("$prefix", prefix)
      .replace("$suffix", suffix)
      .replace("$message", formattedMessage)
  
    // Use Adventure API to create a Component for the renderer
    val chatComponent = Component.text(chatFormat).color(NamedTextColor.WHITE)
    event.renderer((_, _, _, _) => chatComponent)
  }
}

class ChatCommand(plugin: ChatFormatterPlugin) extends CommandExecutor {

  override def onCommand(
      sender: CommandSender,
      command: Command,
      label: String,
      args: Array[String]
  ): Boolean = {
    if (args.isEmpty || !args(0).equalsIgnoreCase("reload")) {
      sender.sendMessage(s"${ChatColor.RED}Usage: /chatformatter reload")
      return true
    }

    plugin.reloadConfig()
    sender.sendMessage(s"${ChatColor.GREEN}ChatFormatter configuration reloaded.")
    true
  }
}
