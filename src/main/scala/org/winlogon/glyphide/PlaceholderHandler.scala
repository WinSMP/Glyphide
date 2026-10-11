// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.Material
import org.bukkit.entity.Player
import org.winlogon.glyphide.hotbar.HotbarPreviewHolder

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickCallback, ClickEvent, HoverEvent}
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

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
    val defaultTokens = List("[item]", "[i]")

    /** The sender's main-hand item with its `SHOW_ITEM` hover. `None` for empty hand. */
    def replacement(player: Player): Option[Component] =
        Option(player.getInventory.getItemInMainHand)
            .filter(_.getType != Material.AIR)
            .map(item => item.displayName().hoverEvent(item.asHoverEvent()))
}

object HotbarPlaceholderHandler extends PlaceholderHandler {
    val id = "hotbar"
    val defaultTokens = List("[hotbar]")

    private val miniMessage = MiniMessage.miniMessage()

    def replacement(player: Player): Option[Component] = {
        val subjectName = player.getName
        // captured here, while the message is being sent, so the button shows the hotbar the
        // sender had at that moment rather than whatever it holds when it is clicked
        val snapshot = HotbarPreviewHolder.capture(player)

        val callback: ClickCallback[Audience] = {
            case viewer: Player => viewer.openInventory(new HotbarPreviewHolder(subjectName, snapshot).inventory)
            case _ => ()
        }

        Some(
            miniMessage.deserialize(
                    s"<gradient:#cd945f:#dfb289>[<player>'s hotbar]</gradient>",
                    Placeholder.unparsed("player", subjectName)
                )
                .hoverEvent(HoverEvent.showText(Component.text(s"Click to preview $subjectName's hotbar.")))
                .clickEvent(ClickEvent.callback(callback, opts =>
                    // Allow repeated clicks - the 1-hour lifetime limits how long Paper retains the callback.
                    opts.uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofHours(1))
                ))
        )
    }
}
