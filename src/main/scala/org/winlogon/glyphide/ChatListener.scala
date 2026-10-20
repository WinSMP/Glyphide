// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import io.papermc.paper.event.player.AsyncChatEvent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.winlogon.glyphide.Formatter.{*, given}

class ChatListener(settingsHolder: SettingsHolder) extends Listener {
    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    def onPlayerChat(event: AsyncChatEvent): Unit = {
        val player = event.getPlayer
        val settings = settingsHolder.settings

        val messageString = PlainTextComponentSerializer.plainText().serialize(event.message())

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

        val context = ChatContext(
            player = player,
            event = event,
            config = settings.config,
            placeholders = settings.placeholders
        )

        // XXX: $username/$world are substituted before MiniMessage parses. Any tag in them would become "live" tags.
        //
        // If both of these values are user-controlled at some point, they should be made
        // to use Component#replaceText like $prefix does.
        val rawChatFormat = settings.chatFormat
            .replace("$username", replacements.username)
            .replace("$world", replacements.world)

        val highlightedMsg = Pipeline(messageString)
            .transform(str => MiniMessageFormatTransform.transform(str, context))
            .transform(comp => SimpleInfoPlaceholderTransform.transform(comp, context))
            .transform(comp => PlaceholderTransform.transform(comp, context))
            .transform(comp => HighlightUrlTransform.transform(comp, context))
            .result

        val chatFormat = Formatter.convertLegacyToMiniMessage(rawChatFormat)

        val component = miniMessage
            .deserialize(chatFormat, resolver)
            .replaceText(builder =>
                builder.matchLiteral("$prefix").replacement(replacements.prefix)
            )
            .replaceText(builder =>
                builder.matchLiteral("$suffix").replacement(replacements.suffix)
            )
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
