// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener, EventPriority}
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.Material

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import io.papermc.paper.event.player.AsyncChatEvent

import Formatter.{given, *}

import java.util.regex.Pattern

class ChatListener(plugin: Plugin) extends Listener {
    private val miniMessage = MiniMessage.miniMessage()
    private val hoverConfigPrefix = "chat.item-placeholder"
    private var isItemPlaceholderEnabled: Boolean = false
    private var itemToken: String = "[item]"
    private var shouldUseHover: Boolean = false

    private val URL_PATTERN: Pattern = Pattern.compile("https?://\\S+")

    private def highlightUrl(message: Component): Component = {
        val config = plugin.getConfig()
        val urlColor = config.getString("chat.url-color", "#4430cc")

        message.replaceText { config =>
            config
                .`match`(URL_PATTERN)
                .replacement((mr, _) => 
                    Component.text(mr.group())
                        .color(TextColor.fromHexString(urlColor))
                        .clickEvent(ClickEvent.openUrl(mr.group()))
                )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    def onPlayerChat(event: AsyncChatEvent): Unit = {
        val player = event.getPlayer
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        val config = plugin.getConfig()

        isItemPlaceholderEnabled = config.getBoolean(s"$hoverConfigPrefix.enabled", false)
        itemToken = config.getString(s"$hoverConfigPrefix.token", "[item]")
        shouldUseHover = config.getBoolean(s"$hoverConfigPrefix.hover", false)

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

        // format message based on permissions
        val msg = formatMessage(player, message)

        val heldOpt: Option[ItemStack] =
            Option(player.getInventory.getItemInMainHand)
                .filter(item => item != null && item.getType != Material.AIR)
                .filter(_ => isItemPlaceholderEnabled)

        val finalMsgComp: Component = heldOpt match {
            case Some(item) =>
                // get its display name (Component) and its hover event
                val nameComp = item.displayName()
                val hoverEvent = item.asHoverEvent()

                // replace any literal placeholder in the message
                msg.replaceText { builder =>
                    builder
                    .matchLiteral(itemToken)
                    .replacement(
                        nameComp.hoverEvent(hoverEvent)
                    )
                    .build()
                }
            case None =>
                // air or no item: leave the message alone
                msg
        }

        val highlightedMsg = highlightUrl(finalMsgComp)

        val chatFormat = convertLegacyToMiniMessage(
            ChatColor.translateAlternateColorCodes('&', rawChatFormat)
        )

        val component = miniMessage
            .deserialize(chatFormat, summon[TagResolver])
            .replaceText(builder => builder.matchLiteral("$prefix").replacement(replacements.prefix))
            .replaceText(builder => builder.matchLiteral("$suffix").replacement(replacements.suffix))
            .replaceText(builder => builder.matchLiteral("$message").replacement(highlightedMsg))

        event.renderer((source, _, _, _) => component)
    }
}

case class Placeholders(
    prefix: Component,
    suffix: Component,
    username: String,
    world: String
)
