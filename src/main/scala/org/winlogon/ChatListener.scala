package org.winlogon

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.plugin.Plugin
import org.fusesource.jansi.{Ansi, AnsiConsole}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import io.papermc.paper.event.player.AsyncChatEvent

import scala.util.matching.Regex

case class Placeholders(
  prefix: String,
  suffix: String,
  username: String,
  message: String,
  world: String
)

class ChatListener(plugin: Plugin) extends Listener {
  @EventHandler
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    val player: Player = event.getPlayer
    // Extract the plain text of the message using PlainTextComponentSerializer
    val message = PlainTextComponentSerializer.plainText().serialize(event.message())

    val replacements = Placeholders(
      prefix = getPrefix(player),
      suffix = getSuffix(player),
      username = player.getName,
      world = player.getWorld.getName,
      message = formatMessage(player, message)
    )

    // Retrieve chat format from config
    var chatFormat = plugin.getConfig.getString("chat.format", "&7$prefix$username$suffix&7 » &f$message")
    // Translate placeholders
    chatFormat = chatFormat
      .replace("$prefix", replacements.prefix)
      .replace("$suffix", replacements.suffix)
      .replace("$username", player.getName)
      .replace("$message", replacements.message)
      .replace("$world", replacements.world)

    // Log with Jansi for terminal colors
    // TODO: fix white not being shown properly and original message showing up in console
    AnsiConsole.systemInstall()
    val ansiPattern: Regex = "&([0-9a-f])".r
    val terminalMessage = ansiPattern.replaceAllIn(chatFormat, m => {
      val colorCode = Integer.parseInt(m.group(1), 16)
      Ansi.ansi().fg(Ansi.Color.values()(colorCode % Ansi.Color.values().length)).toString
    }).replaceAll("&r", Ansi.ansi().reset().toString)
       // Strip unsupported formatting as we have no way to show obfuscated
       // text in the terminal
       .replaceAll("&([k-or])", "")

    // Ensure reset at the end or else I get artefacts of this
    Bukkit.getConsoleSender.sendMessage(terminalMessage + Ansi.ansi().reset().toString)
    AnsiConsole.systemUninstall()

    // Translate Minecraft color codes for in-game chat
    val msg = ChatColor.translateAlternateColorCodes('&', chatFormat)
    // Use Adventure API to create a Component for the renderer
    val chatComponent = Component.text(msg)
    event.renderer((_, _, _, _) => chatComponent)
  }
}
