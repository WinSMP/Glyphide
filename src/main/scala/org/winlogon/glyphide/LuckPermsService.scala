// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.entity.Player

import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User

import scala.util.Try

/** Read-only facade over the LuckPerms API.
  *
  * Isolates all LuckPerms usage so callers never touch LP types directly.
  *
  * Every method returns `None` when LuckPerms is missing, the player is unknown to LP, or the
  * requested meta is unset.
  */
object LuckPermsService {
    extension (p: Player) {
        private def luckPermsUser: Option[User] = Try(LuckPermsProvider.get())
            .toOption
            .flatMap(lp => Option(lp.getUserManager.getUser(p.getUniqueId)))
    }

    extension (user: User) {
        private def prefix: Option[String] = Option(user.getCachedData.getMetaData.getPrefix)
        private def suffix: Option[String] = Option(user.getCachedData.getMetaData.getSuffix)
    }

    /** Resolves the player to their LuckPerms user, if LuckPerms is available and knows them.
      *
      * Returns `None` instead of throwing when the provider is unregistered or the UUID has no LP user.
      */
    def user(player: Player): Option[User] = player.luckPermsUser

    /** The player's formatted prefix from LuckPerms' cached metadata (`meta.prefix`).
      *
      * Reads the cache so it is safe to call on async chat threads.
      *
      * Returns `None` when LP is unavailable or no prefix node is set.
      */
    def prefix(player: Player): Option[String] = player.luckPermsUser.flatMap(_.prefix)

    /** The player's formatted suffix from LuckPerms' cached metadata (`meta.suffix`).
      *
      * Counterpart of [[prefix]]; same cache-only read and `None` semantics.
      */
    def suffix(player: Player): Option[String] = player.luckPermsUser.flatMap(_.suffix)
}
