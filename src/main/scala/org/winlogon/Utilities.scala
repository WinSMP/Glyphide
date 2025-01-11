package org.winlogon

import org.bukkit.entity.Player
import org.bukkit.ChatColor
import net.luckperms.api.{LuckPerms, LuckPermsProvider}
import net.luckperms.api.model.user.User

/**
  * Get the prefix of a player.
  *
  * @param player The player to get the prefix of
  * @return The player's prefix
  */
def getPrefix(player: Player): String = {
  Option(LuckPermsProvider.get().getUserManager.getUser(player.getUniqueId))
    .flatMap(user => Option(user.getCachedData.getMetaData.getPrefix))
    .map(prefix => ChatColor.translateAlternateColorCodes('&', prefix))
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
    .map(suffix => ChatColor.translateAlternateColorCodes('&', suffix))
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
def formatMessage(player: Player, message: String): String = {
  val permissions = Map(
    "chatformatter.color" -> "(?i)&[0-9a-f]",
    "chatformatter.bold" -> "(?i)&l",
    "chatformatter.italic" -> "(?i)&o",
    "chatformatter.underline" -> "(?i)&n",
    "chatformatter.magic" -> "(?i)&k"
  )

  permissions.foldLeft(ChatColor.translateAlternateColorCodes('&', message)) {
    case (msg, (perm, regex)) =>
      if (!player.hasPermission(perm)) msg.replaceAll(regex, "") else msg
  }
}
