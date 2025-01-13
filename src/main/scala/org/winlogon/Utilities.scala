package org.winlogon

import org.bukkit.ChatColor
import org.bukkit.entity.Player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.{LuckPerms, LuckPermsProvider}
import net.luckperms.api.model.user.User

import scala.util.{Try, Success, Failure}

val tagsResolver = TagResolver.builder()
  .resolver(StandardTags.color())
  .resolver(StandardTags.decorations())
  .resolver(StandardTags.gradient())
  .resolver(StandardTags.rainbow())
  .resolver(StandardTags.clickEvent())
  .resolver(StandardTags.hoverEvent())
  .resolver(StandardTags.transition())
  .build()

/**
  * Get the prefix of a player.
  *
  * @param player The player to get the prefix of
  * @return The player's prefix
  */
def getPrefix(player: Player): String = {
  Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
    .flatMap(user => Option(user.getCachedData.getMetaData.getPrefix))
    .map(prefix => toMiniMessage(prefix))
    .getOrElse("")
}

/**
  * Get the suffix of a player.
  * @param player The player to get the suffix of
  * @return The player's suffix
  */
def getSuffix(player: Player): String = {
  Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
    .flatMap(user => Option(user.getCachedData.getMetaData.getSuffix))
    .map(suffix => toMiniMessage(suffix))
    .getOrElse("")
}

/**
  * Format a message for a player, based on their permissions.
  *
  * @param player The player to format the message for
  * @param message The message to format
  * @return The formatted message
  * @see https://minecraft.fandom.com/wiki/Formatting_codes
  */
def formatMessage(player: Player, message: String): Component = {
  if (player.hasPermission("chatformatter.admin")) {
    val mm: MiniMessage = MiniMessage.builder().tags(tagsResolver).build()

    Try {
      mm.deserialize(message, tagsResolver)
    } match {
      case Success(parsedComponent) => parsedComponent
      case Failure(e) =>
        LegacyComponentSerializer.legacyAmpersand().deserialize(message)
    }
  } else {
    LegacyComponentSerializer.legacyAmpersand().deserialize(
      message
        .replaceAll("(?i)&k", "")
        .replaceAll("(?i)&([0-9a-f])", "&$1")
    )
  }
}

def convertLegacyToMiniMessage(input: String): String = {
  val component = LegacyComponentSerializer.legacyAmpersand().deserialize(input)
  MiniMessage.miniMessage().serialize(component)
}

def toMiniMessage(s: String): String = convertLegacyToMiniMessage(s)
