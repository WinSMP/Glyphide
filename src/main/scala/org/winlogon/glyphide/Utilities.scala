package org.winlogon.glyphide

import org.bukkit.ChatColor
import org.bukkit.entity.Player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.model.user.User
import net.luckperms.api.{LuckPerms, LuckPermsProvider}

import scala.util.{Try, Success, Failure}

val advancedTagsResolver = TagResolver
    .builder()
    .resolver(StandardTags.color())
    .resolver(StandardTags.decorations())
    .resolver(StandardTags.gradient())
    .resolver(StandardTags.rainbow())
    .resolver(StandardTags.clickEvent())
    .resolver(StandardTags.hoverEvent())
    .resolver(StandardTags.transition())
    .build()

val basicTagsResolver = TagResolver
    .builder()
    .resolver(StandardTags.color())
    .resolver(StandardTags.decorations())
    .resolver(StandardTags.gradient())
    .build()

/** Get the prefix of a player.
  *
  * @param player The player to get the prefix of
  * @return The player's prefix
  */
def getPrefix(player: Player): Component = {
    Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
        .flatMap(user => Option(user.getCachedData.getMetaData.getPrefix))
        .map(prefix =>
            MiniMessage
                .miniMessage()
                .deserialize(convertLegacyToMiniMessage(prefix), basicTagsResolver)
        )
        .getOrElse(Component.empty())
}

/** Get the suffix of a player.
  * @param player The player to get the suffix of
  * @return The player's suffix
  */
def getSuffix(player: Player): Component = {
    Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
        .flatMap(user => Option(user.getCachedData.getMetaData.getSuffix))
        .map(suffix =>
            MiniMessage
                .miniMessage()
                .deserialize(convertLegacyToMiniMessage(suffix), basicTagsResolver)
        )
        .getOrElse(Component.empty())
}

/** Format a message for a player, based on their permissions.
  *
  * @param player The player to format the message for
  * @param message The message to format
  * @return The formatted message
  * @see https://minecraft.fandom.com/wiki/Formatting_codes
  */
def formatMessage(player: Player, message: String): Component = {
    if (player.hasPermission("glyphide.admin")) {
        // TODO: find better way to escape MiniMessages
        val convertedString = convertLegacyToMiniMessage(message)
            .replaceAll("\\\\>", ">")
            .replaceAll("\\\\<", "<")
        val mm: MiniMessage = MiniMessage.builder().tags(advancedTagsResolver).build()

        Try {
            mm.deserialize(convertedString, advancedTagsResolver)
        } match {
            case Success(parsedComponent) =>
                parsedComponent
            case Failure(e) =>
                LegacyComponentSerializer.legacyAmpersand().deserialize(convertedString)
        }
    } else {
        LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(
                message
                    .replaceAll("(?i)&k", "")
                    .replaceAll("(?i)&([0-9a-f])", "&$1")
            )
    }
}

/** Converts a string with legacy ampersands to MiniMessage-formatted messages.
  *
  * @param input The legacy-formatted string
  * @return The MiniMessage-formatted message
  * @see https://docs.papermc.io/paper/dev/component-api/introduction#legacycomponentserializer
  */
def convertLegacyToMiniMessage(input: String): String = {
    val s = ChatColor.translateAlternateColorCodes('&', input)
    MiniMessage
        .miniMessage()
        .serialize(
            LegacyComponentSerializer.legacySection().deserialize(s)
        )
}
