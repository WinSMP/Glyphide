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
    if (args.isEmpty || !args(0).equalsIgnoreCase("reload")) {
      sender.sendMessage(s"§cUsage§7: /chatformatter §2reload")
      return true
    }

    plugin.reloadConfig()
    sender.sendMessage(s"§7ChatFormatter configuration §3reloaded§7.")
    true
  }
}
