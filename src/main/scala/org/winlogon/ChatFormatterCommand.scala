package org.winlogon

import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}

class ChatFormatterCommand(plugin: ChatFormatter) extends CommandExecutor {
  override def onCommand(
      sender: CommandSender,
      command: Command,
      label: String,
      args: Array[String]
  ): Boolean = {
    // TODO: change colors to match other plugins' theme
    if (args.isEmpty || !args(0).equalsIgnoreCase("reload")) {
      sender.sendMessage(s"${ChatColor.RED}Usage: /chatformatter reload")
      return true
    }

    plugin.reloadConfig()
    sender.sendMessage(s"${ChatColor.GREEN}ChatFormatter configuration reloaded.")
    true
  }
}
