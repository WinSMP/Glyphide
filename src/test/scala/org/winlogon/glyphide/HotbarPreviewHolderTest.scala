// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.inventory.InventoryViewMock
import org.mockbukkit.mockbukkit.{MockBukkit, ServerMock}

import org.winlogon.glyphide.hotbar.{HotbarPreviewHolder, InventoryGuard}

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent, InventoryDragEvent, InventoryType}
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.inventory.{Inventory, ItemStack}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}
import org.junit.jupiter.api.Assertions._

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.configuration.file.YamlConfiguration

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

open class HotbarPreviewHolderTest {
    var server: ServerMock = uninitialized
    var plugin: GlyphideFormatter = uninitialized
    var viewer: PlayerMock = uninitialized

    /** SimpleInventoryViewMock stubs convertSlot, which InventoryClickEvent's constructor calls.
      * The base class implements it, so subclass that instead.
      */
    private class View(player: HumanEntity, top: Inventory, bottom: Inventory)
        extends InventoryViewMock(player, top, bottom, InventoryType.CHEST)

    @BeforeEach
    def setUp(): Unit = {
        server = MockBukkit.mock()
        plugin = MockBukkit.loadSimple(classOf[GlyphideFormatter])
        viewer = server.addPlayer("Viewer")
        server.addWorld(server.addSimpleWorld("world"))
        server.getPluginManager.registerEvents(InventoryGuard(), plugin)
    }

    @AfterEach
    def tearDown(): Unit = MockBukkit.unmock()

    /** A view with `top` on top, as if a player had it open. */
    private def openView(top: Inventory): InventoryViewMock =
        new View(viewer, top, viewer.getInventory)

    private def click(top: Inventory, slot: Int): InventoryClickEvent = {
        val event = new InventoryClickEvent(
            openView(top),
            SlotType.CONTAINER,
            slot,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        )
        server.getPluginManager.callEvent(event)
        event
    }

    private def drag(top: Inventory, slot: Int): InventoryDragEvent = {
        val moved = Map(Integer.valueOf(slot) -> ItemStack(Material.DIAMOND_SWORD)).asJava
        val event = new InventoryDragEvent(openView(top), null, null, false, moved)
        server.getPluginManager.callEvent(event)
        event
    }

    /** Captures now, then builds the window, as the handler does when a message is sent. */
    private def previewFor(name: String): HotbarPreviewHolder = {
        val subject = server.addPlayer(name)
        new HotbarPreviewHolder(name, HotbarPreviewHolder.capture(subject))
    }

    /** The whole point of the holder: a viewer cannot pull the subject's items out. */
    @Test
    def testClickingPreviewIsCancelled(): Unit = {
        val preview = previewFor("Subject").inventory

        assertTrue(click(preview, 0).isCancelled)
    }

    /** Drags never fire InventoryClickEvent, so the guard needs its own handler. */
    @Test
    def testDraggingAcrossPreviewIsCancelled(): Unit = {
        val preview = previewFor("Subject").inventory

        assertTrue(drag(preview, 4).isCancelled)
    }

    /** Scoping by holder must not break every other plugin's menus. */
    @Test
    def testForeignInventoryIsLeftAlone(): Unit = {
        val other = Bukkit.createInventory(null, 9)

        assertFalse(click(other, 0).isCancelled)
        assertFalse(drag(other, 0).isCancelled)
    }

    @Test
    def testPreviewMirrorsSubjectHotbar(): Unit = {
        val subject = server.addPlayer("Subject")
        subject.getInventory.setItem(0, ItemStack.of(Material.DIAMOND_SWORD))
        subject.getInventory.setItem(8, ItemStack.of(Material.DIAMOND_PICKAXE))

        val preview = new HotbarPreviewHolder("Subject", HotbarPreviewHolder.capture(subject)).inventory

        assertEquals(Material.DIAMOND_SWORD, preview.getItem(0).getType)
        assertEquals(Material.DIAMOND_PICKAXE, preview.getItem(8).getType)
        assertEquals(9, preview.getSize)
    }

    /** The bug this guards: a message must show the hotbar the sender had when they wrote it, not
      * whatever they hold when the button is clicked.
      */
    @Test
    def testPreviewDoesNotFollowLaterInventoryChanges(): Unit = {
        val subject = server.addPlayer("Subject")
        subject.getInventory.setItem(0, ItemStack.of(Material.DIAMOND_SWORD))

        val preview = new HotbarPreviewHolder("Subject", HotbarPreviewHolder.capture(subject)).inventory

        // rearrange after the message was sent
        subject.getInventory.setItem(0, ItemStack.of(Material.DIRT))
        subject.getInventory.setItem(3, ItemStack.of(Material.GOLD_INGOT))

        assertEquals(Material.DIAMOND_SWORD, preview.getItem(0).getType)
        assertNull(preview.getItem(3))
    }

    /** Copies, not the subject's own stacks: a viewer must not be able to reach the originals. */
    @Test
    def testPreviewClonesItems(): Unit = {
        val subject = server.addPlayer("Subject")
        val sword = ItemStack.of(Material.DIAMOND_SWORD)
        subject.getInventory.setItem(0, sword)

        val preview = new HotbarPreviewHolder("Subject", HotbarPreviewHolder.capture(subject)).inventory

        assertNotSame(sword, preview.getItem(0))
    }

    // NOTE: Paper's ClickCallbackProviderImpl lives in paper-server, not paper-api, so MockBukkit
    // falls back to Adventure's provider, which degrades ClickEvent.callback to suggestCommand.
    // Real click semantics are therefore untestable here; this only asserts the button is built.
    @Test
    def testHandlerProducesAClickableButton(): Unit = {
        val subject = server.addPlayer("Subject")

        val rendered = HotbarPlaceholderHandler.replacement(subject)

        assertTrue(rendered.isDefined)
        val button = rendered.get
        assertTrue(PlainTextComponentSerializer.plainText().serialize(button).contains("Subject"))
        assertNotNull(button.clickEvent())
        assertNotNull(button.hoverEvent())
    }

    /** Counts how often the transform asks it for a replacement. */
    private class CountingHandler extends PlaceholderHandler {
        val id = "counting"
        val defaultTokens = List("[count]")
        var calls = 0
        def replacement(player: org.bukkit.entity.Player): Option[Component] = {
            calls += 1
            Some(Component.text("replaced"))
        }
    }

    /** Handlers copy sender state, so the transform must not run one for messages that never name
      * its token. A counting stand-in makes the skipped call observable.
      */
    @Test
    def testTransformOnlyRunsHandlersWhoseTokenAppears(): Unit = {
        val counting = new CountingHandler

        def run(message: String): Int = {
            counting.calls = 0
            val context = ChatContext(
                player = viewer,
                event = null,
                config = Configuration(new YamlConfiguration()),
                placeholders = List(ConfiguredPlaceholder(counting, List("[count]")))
            )
            PlaceholderTransform.transform(Component.text(message), context)
            counting.calls
        }

        assertEquals(0, run("nothing to see here"))
        assertEquals(1, run("say [count] please"))
    }
}