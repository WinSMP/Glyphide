// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.configuration.file.FileConfiguration

import scala.jdk.CollectionConverters.*

object Configuration {

    /** One `placeholder.<id>` section, before it is matched against a handler.
      *
      * `tokens` is `None` when the section omits `token:` (or lists nothing), in which case
      * [[PlaceholderHandler.defaultTokens]] applies.
      */
    final case class PlaceholderSection(enabled: Boolean, tokens: Option[List[String]])
}

/** Typed view of config.yml.
  *
  * Every key Glyphide reads is named exactly once, here, so this is the place to look for what the
  * shipped config.yml actually controls.
  *
  * @param config
  *   the loaded configuration; values are snapshotted, so a new instance is needed after a reload
  */
class Configuration(config: FileConfiguration) {
    private val placeholderRoot = "placeholder"

    val urlColor: String = config.getString("url.color", "#6353d4")
    val addHover: Boolean = config.getBoolean("url.hover", true)
    val descLength: Int = config.getInt("url.description-max-length", 15)
    val useHypixelPlaceholders: Boolean = config.getBoolean("use-hypixel-placeholders", false)

    /** The `placeholder.<id>` sections, keyed by id.
      *
      * Read generically rather than one field per placeholder type, so adding a placeholder needs a
      * handler registration ([[PlaceholderRegistry]]) and a config section, never a change here.
      */
    val placeholderSections: Map[String, Configuration.PlaceholderSection] =
        Option(config.getConfigurationSection(placeholderRoot)).toList
            .flatMap(_.getKeys(false).asScala)
            .map { id =>
                val path = s"$placeholderRoot.$id"
                id -> Configuration.PlaceholderSection(
                    // default off: a section that exists but says nothing stays inert
                    enabled = config.getBoolean(s"$path.enabled", false),
                    tokens = Option(config.getStringList(s"$path.token"))
                        .map(_.asScala.toList)
                        .filter(_.nonEmpty)
                )
            }
            .toMap

    val hypixelPlaceholders = Map(
        "o/" -> "<light_purple>( ﾟ◡ﾟ)/</light_purple>",
        "<3" -> "<red>❤</red>"
    )
}
