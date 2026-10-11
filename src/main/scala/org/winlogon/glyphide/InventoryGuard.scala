// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryDragEvent}

/** Cancels item movement in Glyphide's preview windows.
  *
  * Paper has no read-only inventory type, so cancellation is the only supported way to make a
  * GUI immutable. Scoping by holder keeps other plugins' menus working.
  */
class InventoryGuard extends Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    def onInventoryClick(event: InventoryClickEvent): Unit =
        // the top inventory, not the clicked one: also blocks shift-clicking upwards
        if (isPreview(event.getInventory)) event.setCancelled(true)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    def onInventoryDrag(event: InventoryDragEvent): Unit =
        // drags never fire InventoryClickEvent, so they need their own handler
        if (isPreview(event.getInventory)) event.setCancelled(true)

    private def isPreview(inventory: org.bukkit.inventory.Inventory): Boolean =
        // The no-arg getHolder, not getHolder(false): Paper's boolean overload only honours the
        // snapshot flag when the backing inventory is a BlockEntity (CraftInventory#getHolder(boolean)),
        // which a Bukkit.createInventory inventory never is, so the two are equivalent here.
        inventory.getHolder.isInstanceOf[HotbarPreviewHolder]
}