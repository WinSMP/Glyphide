// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.{Material, Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener, EventPriority}
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

import net.kyori.adventure.text.{Component, TextReplacementConfig}
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import io.papermc.paper.event.player.AsyncChatEvent

import Formatter.{given, *}

import scala.jdk.CollectionConverters.*
import java.util.regex.Pattern
import java.util.{List => JavaList}

class ChatListener(plugin: Plugin) extends Listener {
    private val miniMessage = MiniMessage.miniMessage()
    private val formatUrl = FormatUrl()

    private val hoverConfigPrefix = "chat.item-placeholder"
    private var isItemPlaceholderEnabled: Boolean = false
    private var itemTokens: List[String] = List("[item]")

    private val URL_PATTERN: Pattern = Pattern.compile("https?://\\S+")

    private def generateHoverText(
        default: Component, info: UrlInformation, descLength: Int
    ): Component = {
        val hoverText = info.description match {
            case Some(desc) => {
                val lineEnding = if (desc.length > descLength) "..." else ""
                val description = s"${desc.take(descLength)}$lineEnding"
                s"${info.title} — $description"
            }
            case None => info.title
        }
        default.hoverEvent(HoverEvent.showText(Component.text(hoverText)))
    }

    private def highlightUrl(message: Component): Component = {
        val config = plugin.getConfig()
        val urlColor = config.getString("chat.url.color", "#6353d4")
        val addHover = config.getBoolean("chat.url.hover", true)
        val descLength = config.getInt("chat.url.description-max-length", 15)

        message.replaceText { config =>
            config
                .`match`(URL_PATTERN)
                .replacement((mr, _) => {
                    val text = Component.text(mr.group())
                        .color(TextColor.fromHexString(urlColor))
                        .clickEvent(ClickEvent.openUrl(mr.group()))

                    formatUrl.getUrlInformation(mr.group()) match {
                        case Some(info) if addHover => generateHoverText(text, info, descLength)
                        case _ => text
                    }
                })
            }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    def onPlayerChat(event: AsyncChatEvent): Unit = {
        val player = event.getPlayer
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        val config = plugin.getConfig()

        isItemPlaceholderEnabled = config.getBoolean(s"$hoverConfigPrefix.enabled", false)
        itemTokens = Option(config.getStringList(s"$hoverConfigPrefix.tokens"))
            .getOrElse(JavaList.of("[item]"))
            .asScala
            .toList

        val playerHasPermission = player.hasPermission(Formatter.Permission.Admin.name)

        val resolver: TagResolver = if (playerHasPermission) {
            Formatter.advancedResolver
        } else {
            Formatter.basicResolver
        }

        val replacements = Placeholders(
            prefix = getPrefix(player)(using resolver),
            suffix = getSuffix(player)(using resolver),
            username = player.getName,
            world = player.getWorld.getName
        )

        val rawChatFormat = plugin.getConfig
            .getString("chat.format", "$prefix $username > $message")
            .replace("$username", replacements.username)
            .replace("$world", replacements.world)

        // format message based on permissions
        val msg = formatMessageByPermission(player, message)

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
                itemTokens.foldLeft(msg) { (component, token) =>
                    component.replaceText { builder =>
                        builder
                            .matchLiteral(token)
                            .replacement(nameComp.hoverEvent(hoverEvent))
                            .build()
                    }
                }
            case None =>
                // air or no item: leave the message alone
                msg
        }

        val highlightedMsg = highlightUrl(finalMsgComp)

        val chatFormat = convertLegacyToMiniMessage(rawChatFormat)

        val component = miniMessage
            .deserialize(chatFormat, resolver)
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
