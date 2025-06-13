package org.winlogon.glyphide

import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.{MockBukkit, ServerMock}

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.chat.ChatRenderer

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.{ChatColor, Material}
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{mock, verify, when}

import java.util.UUID

class ChatListenerTest {

    var server: ServerMock = _
    var plugin: GlyphideFormatter = _
    var player: PlayerMock = _

    @BeforeEach
    def setUp(): Unit = {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(classOf[GlyphideFormatter])
        player = server.addPlayer()
        val mockWorld = server.addSimpleWorld("world")
        server.addWorld(mockWorld)
    }

    @AfterEach
    def tearDown(): Unit = {
        MockBukkit.unmock()
    }

    @Test
    def testBasicChatFormatting(): Unit = {
        // setup config
        plugin.getConfig.set("chat.format", "$prefix $username > $message")
        plugin.saveConfig()
        plugin.reloadConfig()

        // create listener and event
        val listener = new ChatListener(plugin)
        val event = mock(classOf[AsyncChatEvent])
        when(event.getPlayer).thenReturn(player)
        when(event.message()).thenReturn(Component.text("Hello"))
        when(event.viewers()).thenReturn(java.util.Collections.singleton(player))

        // trigger event
        listener.onPlayerChat(event)

        // capture rendered component
        val captor = ArgumentCaptor.forClass(classOf[ChatRenderer])
        verify(event).renderer(captor.capture())
        val rendered = captor.getValue.render(player, Component.text(""), Component.text(""), player)

        // verify basic formatting
        val plainText = PlainTextComponentSerializer.plainText().serialize(rendered)
        assertTrue(plainText.contains("TestPlayer > Hello"))
    }

    @Test
    def testItemPlaceholder(): Unit = {
        // enable item placeholder
        plugin.getConfig.set("chat.item-placeholder.enabled", true)
        plugin.getConfig.set("chat.item-placeholder.token", "[item]")
        plugin.saveConfig()
        plugin.reloadConfig()

        // give player an item
        val diamondSword = new ItemStack(Material.DIAMOND_SWORD)
        player.getInventory.setItemInMainHand(diamondSword)

        // create listener and event
        val listener = ChatListener(plugin)
        val event = mock(classOf[AsyncChatEvent])
        when(event.getPlayer).thenReturn(player)
        when(event.message()).thenReturn(Component.text("Check [item]"))
        when(event.viewers()).thenReturn(java.util.Collections.singleton(player))

        // trigger event
        listener.onPlayerChat(event)

        // capture rendered component
        val captor = ArgumentCaptor.forClass(classOf[ChatRenderer])
        verify(event).renderer(captor.capture())
        val rendered = captor.getValue.render(player, Component.text(""), Component.text(""), player)

        // verify item placeholder replacement
        val plainText = PlainTextComponentSerializer.plainText().serialize(rendered)
        assertTrue(plainText.contains("Diamond Sword"))

        // verify hover event
        val hoverEvent = rendered.children().get(0).hoverEvent()
        assertNotNull(hoverEvent)
        assertEquals(HoverEvent.Action.SHOW_ITEM, hoverEvent.action())
    }

    @Test
    def testUrlHighlighting(): Unit = {
        // enable URL highlighting
        plugin.getConfig.set("chat.url.color", "#FF0000")
        plugin.getConfig.set("chat.url.hover", true)
        plugin.saveConfig()
        plugin.reloadConfig()

        // create listener and event
        val listener = new ChatListener(plugin)
        val event = mock(classOf[AsyncChatEvent])
        when(event.getPlayer).thenReturn(player)
        when(event.message()).thenReturn(Component.text("Visit https://example.com"))
            when(event.viewers()).thenReturn(java.util.Collections.singleton(player))

            // trigger event
            listener.onPlayerChat(event)

            // capture rendered component
            val captor = ArgumentCaptor.forClass(classOf[ChatRenderer])
            verify(event).renderer(captor.capture())
            val rendered = captor.getValue.render(player, Component.text(""), Component.text(""), player)

            // verify URL styling
            val urlPart = rendered.children().get(1)
            assertEquals(TextColor.fromHexString("#FF0000"), urlPart.color())
            assertEquals(ClickEvent.Action.OPEN_URL, urlPart.clickEvent().action())
            assertTrue(urlPart.hoverEvent().value().toString.contains("example.com"))
    }

    @Test
    def testAdminFormatting(): Unit = {
        // give admin permissions
        player.addAttachment(plugin, "glyphide.admin", true)

        // create listener and event
        val listener = new ChatListener(plugin)
        val event = mock(classOf[AsyncChatEvent])
        when(event.getPlayer).thenReturn(player)
        when(event.message()).thenReturn(Component.text("<red>Admin message</red>"))
        when(event.viewers()).thenReturn(java.util.Collections.singleton(player))

        // trigger event
        listener.onPlayerChat(event)

        // capture rendered component
        val captor = ArgumentCaptor.forClass(classOf[ChatRenderer])
        verify(event).renderer(captor.capture())
        val rendered = captor.getValue.render(player, Component.text(""), Component.text(""), player)

        // verify advanced formatting
        assertTrue(rendered.style().hasDecoration(net.kyori.adventure.text.format.TextDecoration.BOLD))
        assertEquals(TextColor.fromHexString("#FF5555"), rendered.color())
    }
}
