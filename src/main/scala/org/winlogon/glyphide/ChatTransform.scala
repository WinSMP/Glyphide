package org.winlogon.glyphide

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import io.papermc.paper.event.player.AsyncChatEvent

import java.util.regex.Pattern

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

trait ChatTransform[I, O] {
    def transform(input: I, context: ChatContext): O
}

final case class ChatContext(
    player: Player,
    event: AsyncChatEvent,
    config: Configuration,
)

final class Pipeline[A](private val value: A) {
    def transform[B](f: A => B): Pipeline[B] = {
        Pipeline(f(value))
    }

    def result: A = value
}

object AmpersandTransform extends ChatTransform[String, String] {
    def transform(input: String, context: ChatContext): String = {
        return Formatter.convertLegacyToMiniMessage(input)
    }
}

object ItemPlaceholderTransform extends ChatTransform[Component, Component] {
    def transform(input: Component, context: ChatContext): Component = {
        val heldOpt: Option[ItemStack] =
            Option(context.player.getInventory.getItemInMainHand)
                .filter(item => item != null && item.getType != Material.AIR)
                .filter(_ => context.config.isItemPlaceholderEnabled)

        val msg = input

        val finalMsgComp: Component = heldOpt match {
            case Some(item) =>
                // get its display name (Component) and its hover event
                val nameComp = item.displayName()
                val hoverEvent = item.asHoverEvent()

                // replace any literal placeholder in the message
                context.config.itemTokens.foldLeft(msg) { (component, token) =>
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

        return finalMsgComp
    }
}

object HighlightUrlTransform extends ChatTransform[Component, Component] {
    private val formatUrl = FormatUrl()

    def transform(input: Component, context: ChatContext): Component = {
        input.replaceText { config =>
            config
                .`match`(URL_PATTERN)
                .replacement((mr, _) => {
                    val text = Component.text(mr.group())
                        .color(TextColor.fromHexString(context.config.urlColor))
                        .clickEvent(ClickEvent.openUrl(mr.group()))

                    // Get URL information from the cache
                    formatUrl.getUrlInformation(mr.group()) match {
                        // Cache hit
                        case Some(info) if context.config.addHover =>
                            generateHoverText(text, info, context.config.descLength)
                        // Cache miss - fetch runs async to avoid blocking chat;
                        // hover appears from the next message onward.
                        // Rationale: FormatUrl.getUrlInformation contracts.
                        case _ => text
                    }
                })
            }
    }
}

object SimpleInfoPlaceholderTransform extends ChatTransform[String, String] {
    def transform(input: String, context: ChatContext): String = {
        if (context.config.useHypixelPlaceholders) {
            context.config.hypixelPlaceholders.foldLeft(input) { case (acc, (placeholder, replacement)) =>
                acc.replace(placeholder, replacement)
            }
        } else {
            input
        }
    }
}

object MiniMessageFormatTransform extends ChatTransform[String, Component] {
    def transform(input: String, context: ChatContext): Component = Formatter.formatMessageByPermission(context.player, input)
}
