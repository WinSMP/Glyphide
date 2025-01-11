package org.winlogon

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener, EventPriority}
import org.bukkit.plugin.Plugin
import org.fusesource.jansi.{Ansi, AnsiConsole}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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
  val colorMap: Map[Char, Ansi.Color] = Map(
    '0' -> Ansi.Color.BLACK,
    '1' -> Ansi.Color.BLUE,
    '2' -> Ansi.Color.GREEN,
    '3' -> Ansi.Color.CYAN,
    '4' -> Ansi.Color.RED,
    '5' -> Ansi.Color.MAGENTA,
    '6' -> Ansi.Color.YELLOW,
    '7' -> Ansi.Color.WHITE,
    '8' -> Ansi.Color.BLACK,
    '9' -> Ansi.Color.BLUE,
    'a' -> Ansi.Color.GREEN,
    'b' -> Ansi.Color.CYAN,
    'c' -> Ansi.Color.RED,
    'd' -> Ansi.Color.MAGENTA,
    'e' -> Ansi.Color.YELLOW,
    'f' -> Ansi.Color.WHITE
  )

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    val player: Player = event.getPlayer
    val message = PlainTextComponentSerializer.plainText().serialize(event.message())

    val replacements = Placeholders(
      prefix = getPrefix(player),
      suffix = getSuffix(player),
      username = player.getName,
      world = player.getWorld.getName,
      message = formatMessage(player, message)
    )

    var chatFormat = plugin.getConfig.getString("chat.format", "&7$prefix$username$suffix&7 » &f$message")
    chatFormat = chatFormat
      .replace("$prefix", replacements.prefix)
      .replace("$suffix", replacements.suffix)
      .replace("$username", player.getName)
      .replace("$message", replacements.message)
      .replace("$world", replacements.world)

    AnsiConsole.systemInstall()

    val resetCode = Ansi.ansi().reset().toString
    val ansiPattern: Regex = "&([0-9a-f])".r
    val terminalMessage = ansiPattern.replaceAllIn(chatFormat, m => {
      val colorCode = m.group(1).charAt(0)
      val ansiColor = colorMap.getOrElse(colorCode, Ansi.Color.DEFAULT)
      Ansi.ansi().fg(ansiColor).toString
    }).replaceAll("&r", resetCode)
      .replaceAll("&([k-or])", "")

    Bukkit.getConsoleSender.sendMessage(terminalMessage + resetCode)
    AnsiConsole.systemUninstall()

    val hexPattern: Regex = "<#[0-9a-fA-F]{6}>".r
    val coloredMessage = hexPattern.replaceAllIn(chatFormat, m => {
      val hexColor = m.matched.substring(2, 8)
      TextColor.fromHexString("#" + hexColor).asHexString()
    })

    val componentMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(coloredMessage)

    event.renderer((_, _, _, _) => componentMessage)
  }
}
