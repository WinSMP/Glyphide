// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener, EventPriority}
import org.bukkit.plugin.Plugin

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import io.papermc.paper.event.player.AsyncChatEvent

import Formatter.{given, *}

class ChatListener(plugin: Plugin) extends Listener {
    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    def onPlayerChat(event: AsyncChatEvent): Unit = {
        val player = event.getPlayer
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        given TagResolver = Formatter.basicResolver

        val replacements = Placeholders(
            prefix = getPrefix(player),
            suffix = getSuffix(player),
            username = player.getName,
            world = player.getWorld.getName
        )

        val rawChatFormat = plugin.getConfig
            .getString("chat.format", "$prefix $username > $message")
            .replace("$username", replacements.username)
            .replace("$world", replacements.world)

        val chatFormat = convertLegacyToMiniMessage(
            ChatColor.translateAlternateColorCodes('&', rawChatFormat)
        )
        val formattedMessageComponent = formatMessage(player, message)

        val component = miniMessage
            .deserialize(chatFormat, summon[TagResolver])
            .replaceText(builder => builder.matchLiteral("$prefix").replacement(replacements.prefix))
            .replaceText(builder => builder.matchLiteral("$suffix").replacement(replacements.suffix))
            .replaceText(builder => builder.matchLiteral("$message").replacement(formattedMessageComponent))

        event.renderer((source, _, _, _) => component)
    }
}

case class Placeholders(
    prefix: Component,
    suffix: Component,
    username: String,
    world: String
)
