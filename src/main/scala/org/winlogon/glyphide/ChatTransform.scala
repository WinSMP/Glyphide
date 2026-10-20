package org.winlogon.glyphide

import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Material

import java.util.regex.Pattern

private val URL_PATTERN: Pattern = Pattern.compile("https?://\\S+")

private def generateHoverText(
    default: Component,
    info: UrlInformation,
    descLength: Int
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
    placeholders: List[ConfiguredPlaceholder]
)

final class Pipeline[A](private val value: A) {
    def transform[B](f: A => B): Pipeline[B] = {
        Pipeline(f(value))
    }

    def result: A = value
}

object AmpersandTransform extends ChatTransform[String, String] {
    def transform(input: String, context: ChatContext): String = {
        Formatter.convertLegacyToMiniMessage(input)
    }
}

object HighlightUrlTransform extends ChatTransform[Component, Component] {
    private val formatUrl = FormatUrl()

    def transform(input: Component, context: ChatContext): Component = {
        input.replaceText { config =>
            config
                .`match`(URL_PATTERN)
                .replacement((mr, _) => {
                    val text = Component
                        .text(mr.group())
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

/** Substitutes every enabled placeholder token with what its handler produces for the sender.
  *
  * One transform covers all placeholder types, so the pipeline length and the config format stay
  * put as types are added.
  */
object PlaceholderTransform extends ChatTransform[Component, Component] {
    private val plainText = PlainTextComponentSerializer.plainText()

    def transform(input: Component, context: ChatContext): Component = {
        val text = plainText.serialize(input)

        context.placeholders.foldLeft(input) { (component, placeholder) =>
            // handlers read sender state (and may copy it), so only run the ones this message names
            if (!placeholder.tokens.exists(text.contains)) component
            else
                placeholder.handler
                    .replacement(context.player)
                    .fold(component)(replacement =>
                        substitute(component, placeholder.tokens, replacement)
                    )
        }
    }

    private def substitute(component: Component, tokens: List[String], replacement: Component): Component =
        tokens.foldLeft(component) { (acc, token) =>
            acc.replaceText(builder => builder.matchLiteral(token).replacement(replacement).build())
        }
}


object SimpleInfoPlaceholderTransform extends ChatTransform[Component, Component] {
    private val miniMessage = MiniMessage.miniMessage()

    def transform(input: Component, context: ChatContext): Component = {
        if (!context.config.useHypixelPlaceholders) {
            return input
        }

        context.config.hypixelPlaceholders.foldLeft(input) {
            case (component, (token, markup)) =>
                val replacement = miniMessage.deserialize(markup, TagResolver.standard())
                component.replaceText(builder => builder.matchLiteral(token).replacement(replacement))
        }
    }
}

object MiniMessageFormatTransform extends ChatTransform[String, Component] {
    def transform(input: String, context: ChatContext): Component =
        Formatter.formatMessageByPermission(context.player, input)
}
