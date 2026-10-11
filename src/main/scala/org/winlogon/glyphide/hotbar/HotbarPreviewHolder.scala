// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide.hotbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.{NamedTextColor, Style, TextDecoration}
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.{Inventory, InventoryHolder, ItemStack}

/** Read-only 9-slot window showing one player's hotbar as it was when their message was sent.
  *
  * The holder is this GUI's identity: `InventoryGuard` keys off it to cancel clicks, so the title
  * never has to be matched on.
  */
final class HotbarPreviewHolder(subjectName: String, snapshot: Array[Option[ItemStack]])
    extends InventoryHolder {

    val inventory: Inventory = {
        val preview = Bukkit.createInventory(this, HotbarPreviewHolder.Size, title)
        for (slot <- 0 until HotbarPreviewHolder.Size) {
            // cloned again per click: the same callback can fire many times, so the stored
            // snapshot must not be reachable from anything the viewer can touch
            snapshot(slot).foreach(item => preview.setItem(slot, item.clone()))
        }
        preview
    }

    private def title: Component =
        Component
            .text(s"$subjectName's Hotbar")
            // inventory titles render italic by default otherwise
            .style(
                Style
                    .style()
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .build()
            )

    override def getInventory: Inventory = inventory
}

object HotbarPreviewHolder {

    /** One hotbar row. */
    val Size = 9

    /** Copies the player's hotbar.
      *
      * Keeps the returned value consistent regardless of whether the player rearranges the items
      * afterward.
      */
    def capture(player: Player): Array[Option[ItemStack]] =
        Array.tabulate(Size)(slot =>
            Option(player.getInventory.getItem(slot))
                .filter(_.getType.isItem)
                .map(_.clone())
        )
}
