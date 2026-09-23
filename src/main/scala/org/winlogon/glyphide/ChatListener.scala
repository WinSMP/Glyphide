// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import io.papermc.paper.event.player.AsyncChatEvent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.Material
import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.winlogon.glyphide.Formatter.{*, given}

import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*

class ChatListener(plugin: Plugin) extends Listener {
    private val miniMessage = MiniMessage.miniMessage()
    private val formatUrl = FormatUrl()
    private val formatConfig = Configuration(plugin.getConfig)

    // TODO: also match `http://`
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
        message.replaceText { config =>
            config
                .`match`(URL_PATTERN)
                .replacement((mr, _) => {
                    val text = Component.text(mr.group())
                        .color(TextColor.fromHexString(formatConfig.urlColor))
                        .clickEvent(ClickEvent.openUrl(mr.group()))

                    // Get URL information from the cache
                    formatUrl.getUrlInformation(mr.group()) match {
                        // Cache hit
                        case Some(info) if formatConfig.addHover =>
                            generateHoverText(text, info, formatConfig.descLength)
                        // Cache miss - fetch runs async to avoid blocking chat;
                        // hover appears from the next message onward.
                        // Rationale: FormatUrl.getUrlInformation contracts.
                        case _ => text
                    }
                })
            }
    }

    private def replaceHypixelPlaceholders(message: String): String = {
        formatConfig.hypixelPlaceholders.foldLeft(message) { case (acc, (placeholder, replacement)) =>
            acc.replace(placeholder, replacement)
        }
    }

    // TODO: add a proper architecture where: input source -> (one or more) transform -> sink
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    def onPlayerChat(event: AsyncChatEvent): Unit = {
        val player = event.getPlayer
        var message = PlainTextComponentSerializer.plainText().serialize(event.message())

        val playerHasPermission = player.hasPermission(Formatter.Permission.Admin.name)

        if (formatConfig.useHypixelPlaceholders) {
           message = replaceHypixelPlaceholders(message)
        }

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
                .filter(_ => formatConfig.isItemPlaceholderEnabled)

        val finalMsgComp: Component = heldOpt match {
            case Some(item) =>
                // get its display name (Component) and its hover event
                val nameComp = item.displayName()
                val hoverEvent = item.asHoverEvent()

                // replace any literal placeholder in the message
                formatConfig.itemTokens.foldLeft(msg) { (component, token) =>
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
