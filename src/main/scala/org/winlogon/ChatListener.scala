package org.winlogon

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

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
      username = player.getName, // Added the missing username argument
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

    val msg = ChatColor.translateAlternateColorCodes('&', chatFormat)
    // Use Adventure API to create a Component for the renderer
    val chatComponent = Component.text(msg).color(NamedTextColor.WHITE)
    event.renderer((_, _, _, _) => chatComponent)
  }
}
