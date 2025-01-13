package org.winlogon

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener, EventPriority}
import org.bukkit.plugin.Plugin
import org.fusesource.jansi.{Ansi, AnsiConsole}

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
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
  val miniMessage = MiniMessage.miniMessage()

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    val player: Player = event.getPlayer
    val message = PlainTextComponentSerializer.plainText().serialize(event.message())
  
    val replacements = Placeholders(
      prefix = getPrefix(player),
      suffix = getSuffix(player),
      username = player.getName,
      world = player.getWorld.getName,
      // Placeholder, will be replaced with Component
      message = "",
    )
  
    val chatFormatRaw = plugin.getConfig.getString("chat.format", "$prefix $username &7» &f$message")
    val chatFormat = chatFormatRaw
      .replace("$prefix", replacements.prefix)
      .replace("$suffix", replacements.suffix)
      .replace("$username", replacements.username)
      .replace("$world", replacements.world)
      .replace("$message", replacements.message)

      val chatFormatMini = convertLegacyToMiniMessage(chatFormat)
      val formattedMessageComponent = formatMessage(player, message)

      val finalComponent = MiniMessage.miniMessage().deserialize(chatFormatMini, tagsResolver)
        .replaceText(builder => builder.matchLiteral("$message").replacement(formattedMessageComponent))

      val component = miniMessage.deserialize(finalComponent, tagsResolver)

      Bukkit.getConsoleSender.sendMessage(finalComponent)
      event.message()
  }
}
