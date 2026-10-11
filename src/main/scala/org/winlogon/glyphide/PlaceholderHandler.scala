// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.Material
import org.bukkit.entity.Player

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickCallback, ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.{NamedTextColor, TextDecoration}

import java.time.Duration

/** Builds the replacement for one kind of chat placeholder token. */
trait PlaceholderHandler {

    /** The `placeholder.<id>` section this handler implements. */
    def id: String

    /** Tokens to use when the config omits `token:`, e.g. `List("[item]")`. */
    def defaultTokens: List[String]

    /** What to substitute for each token. `None` leaves the literal token in the message.
      * Runs on the async chat thread — must not block. */
    def replacement(player: Player): Option[Component]
}


object ItemPlaceholderHandler extends PlaceholderHandler {
    val id = "item"
    val defaultTokens = List("[item]")

    /** The sender's main-hand item with its `SHOW_ITEM` hover. `None` for empty hand. */
    def replacement(player: Player): Option[Component] =
        Option(player.getInventory.getItemInMainHand)
            .filter(_.getType != Material.AIR)
            .map(item => item.displayName().hoverEvent(item.asHoverEvent()))
}

object HotbarPlaceholderHandler extends PlaceholderHandler {
    val id = "hotbar"
    val defaultTokens = List("[hotbar]")

    def replacement(player: Player): Option[Component] = {
        val subjectName = player.getName
        // captured here, while the message is being sent, so the button shows the hotbar the
        // sender had at that moment rather than whatever it holds when it is clicked
        val snapshot = HotbarPreviewHolder.capture(player)

        val callback: ClickCallback[Audience] = audience => audience match {
            case viewer: Player => viewer.openInventory(new HotbarPreviewHolder(subjectName, snapshot).inventory)
            case _ => ()
        }

        Some(
            Component
                .text(s"[$subjectName's Hotbar]", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text(s"Click to preview $subjectName's hotbar.")))
                // Adventure defaults to uses=1, so the button would die after a single click.
                // UNLIMITED_USES makes takeUse() a no-op server-side; the lifetime bounds how long
                // the entry sits in Paper's callback map, since a chat line is clickable for seconds.
                .clickEvent(ClickEvent.callback(callback, opts =>
                    opts.uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofHours(1))
                ))
        )
    }
}
