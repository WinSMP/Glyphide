package org.winlogon.glyphide

import org.bukkit.configuration.file.FileConfiguration

import java.util.List as JavaList
import scala.jdk.CollectionConverters.*

class Configuration(config: FileConfiguration) {
    private val hoverConfigPrefix = "item-placeholder"

    val urlColor: String = config.getString("url.color", "#6353d4")
    val addHover: Boolean = config.getBoolean("url.hover", true)
    val descLength: Int = config.getInt("url.description-max-length", 15)
    val isItemPlaceholderEnabled: Boolean = config.getBoolean(s"$hoverConfigPrefix.enabled", false)
    val itemTokens: List[String] = Option(config.getStringList(s"$hoverConfigPrefix.tokens"))
            .getOrElse(JavaList.of("[item]"))
            .asScala
            .toList
    val useHypixelPlaceholders: Boolean = config.getBoolean("use-hypixel-placeholders", false)

    val hypixelPlaceholders = Map(
        "o/" -> "<light_purple>( ﾟ◡ﾟ)/</light_purple>"
    )
}
