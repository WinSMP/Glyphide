// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.configuration.file.FileConfiguration

import java.util.logging.Logger

/** Everything Glyphide reads from config.yml, as one consistent immutable value.
  *
  * Grouping these keeps a reload atomic: a message is rendered either entirely against the old
  * values
  * or entirely against the new ones, never against a mix.
  */
final case class Settings(
    config: Configuration,
    chatFormat: String,
    placeholders: List[ConfiguredPlaceholder]
)

object Settings {

    private val defaultChatFormat = "$prefix $username > $message"

    /** Parses config.yml once and resolves the placeholder table against it.
      *
      * @param fileConfig
      *   the loaded configuration, i.e. `plugin.getConfig`
      * @param logger
      *   receives config problems found while resolving placeholders
      */
    def load(fileConfig: FileConfiguration, logger: Logger): Settings = {
        val config = Configuration(fileConfig)

        Settings(
            config = config,
            // read alongside everything else so a reload cannot change this later in the same
            //message
            chatFormat = fileConfig.getString("chat.format", defaultChatFormat),
            placeholders = PlaceholderRegistry.resolve(config, logger)
        )
    }
}
