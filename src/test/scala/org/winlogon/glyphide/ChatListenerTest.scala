// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.{MockBukkit, ServerMock}

import io.papermc.paper.event.player.AsyncChatEvent

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}
import org.junit.jupiter.api.Assertions._

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

import java.util.Collections

open class ChatListenerTest {
    var server: ServerMock = uninitialized
    var plugin: GlyphideFormatter = uninitialized
    var player: PlayerMock = uninitialized

    @BeforeEach
    def setUp(): Unit = {
        server = MockBukkit.mock()
        plugin = MockBukkit.loadSimple(classOf[GlyphideFormatter])
        player = server.addPlayer("TestPlayer")

        val mockWorld = server.addSimpleWorld("world")
        server.addWorld(mockWorld)
    }

    @AfterEach
    def tearDown(): Unit = {
        MockBukkit.unmock()
    }

    /** Fires a real AsyncChatEvent through the mock server, returns the rendered output.
      *
      * Builds a fresh ChatListener per call because Configuration snapshots config values at
      * construction. The listener must be created after the test sets its keys.
      */
    private def renderMessage(message: Component): Component = {
        server.getPluginManager.registerEvents(ChatListener(plugin), plugin)

        val viewers = Collections.singleton[Audience](player)
        val event = new AsyncChatEvent(
            false,
            player,
            viewers,
            (_, _, _, _) => Component.empty(),
            message,
            message,
            SignedMessage.system("test", message)
        )
        server.getPluginManager.callEvent(event)

        event.renderer().render(player, Component.text(""), Component.text(""), player)
    }

    // The final chat component nests $message replacements, so search recursively.
    private def findComponent(root: Component, pred: Component => Boolean): Option[Component] = {
        if (pred(root)) Some(root)
        else root.children().asScala.flatMap(child => findComponent(child, pred)).headOption
    }

    @Test
    def testBasicChatFormatting(): Unit = {
        plugin.getConfig.set("chat.format", "$prefix $username > $message")
        plugin.saveConfig()
        plugin.reloadConfig()

        val rendered = renderMessage(Component.text("Hello"))

        val plainText = PlainTextComponentSerializer.plainText().serialize(rendered)
        assertTrue(plainText.contains("TestPlayer > Hello"))
    }

    // NOTE: MockBukkit 4.115.0 has not implemented item hover serialization. The assertions below
    // never run as of writing but validate the path once upstream implements it.
    @Test
    def testItemPlaceholder(): Unit = {
        plugin.getConfig.set("item-placeholder.enabled", true)
        plugin.getConfig.set("item-placeholder.token", "[item]")
        plugin.saveConfig()
        plugin.reloadConfig()

        val diamondSword = ItemStack.of(Material.DIAMOND_SWORD)
        player.getInventory.setItemInMainHand(diamondSword)

        val rendered = renderMessage(Component.text("Check [item]"))

        val plainText = PlainTextComponentSerializer.plainText().serialize(rendered)
        assertTrue(plainText.contains("Diamond Sword"))
        assertFalse(plainText.contains("[item]"))

        val itemPart = findComponent(
            rendered,
            c => c.hoverEvent() != null && c.hoverEvent().action() == HoverEvent.Action.SHOW_ITEM
        )
        assertTrue(itemPart.isDefined)
    }

    @Test
    def testUrlHighlighting(): Unit = {
        // top-level keys, no `chat.` prefix
        plugin.getConfig.set("url.color", "#FF0000")
        plugin.getConfig.set("url.hover", true)
        plugin.saveConfig()
        plugin.reloadConfig()

        val rendered = renderMessage(Component.text("Visit https://example.com"))

        val urlPart = findComponent(
            rendered,
            c => c.clickEvent() != null && c.clickEvent().action() == ClickEvent.Action.OPEN_URL
        )
        assertTrue(urlPart.isDefined)
        assertEquals(TextColor.fromHexString("#FF0000"), urlPart.get.color())
        assertTrue(urlPart.get.clickEvent().value().toString.contains("https://example.com"))
    }

    @Test
    def testAdminFormatting(): Unit = {
        player.addAttachment(plugin, "glyphide.admin", true)

        val rendered = renderMessage(Component.text("<red>Admin message</red>"))

        // interpreted, not echoed literally; <red> == #FF5555
        val plainText = PlainTextComponentSerializer.plainText().serialize(rendered)
        assertFalse(plainText.contains("<red>"))
        val messagePart = findComponent(
            rendered,
            c => PlainTextComponentSerializer.plainText().serialize(c) == "Admin message"
        )
        assertTrue(messagePart.isDefined)
        assertEquals(TextColor.fromHexString("#FF5555"), messagePart.get.color())
    }
}
