package org.winlogon

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.plugin.Plugin

case class Placeholders(
  prefix: String,
  suffix: String,
  username: String,
  message: String,
  world: String
)

class ChatListener(plugin: Plugin) extends Listener {

  // Adventure Legacy Component Serializer with hex color support
  private val legacySerializer: LegacyComponentSerializer = LegacyComponentSerializer.builder()
    .character('&') // Use '&' as the color code character
    .hexColors()    // Enable hex color support
    .build()


  @EventHandler
  def onPlayerChat(event: AsyncChatEvent): Unit = {
    val player: Player = event.getPlayer
    val rawMessage = event.message() // Get the raw message as a Component
    val plainMessage = Component.text().content(rawMessage).build() // Convert to plain text


    val replacements = Placeholders(
      prefix = getPrefix(player),
      suffix = getSuffix(player),
      username = player.getName,
      world = player.getWorld.getName,
      message = formatMessage(player, plainMessage)
    )

    // Retrieve chat format from config
    val chatFormat = plugin.getConfig.getString("chat.format", "&7$prefix$username$suffix&7 » &f$message")

    // Replace placeholders in the format
    val formattedMessage = chatFormat
      .replace("$prefix", getPrefix(player))
      .replace("$suffix", getSuffix(player))
      .replace("$username", player.getName)
      .replace("$message", formatMessage(player, plainMessage))

    // Deserialize the formatted string into a Component with color support
    val chatComponent: Component = legacySerializer.deserialize(formattedMessage)

    // Set the formatted Component as the event message
    event.message(chatComponent)
  }
}

