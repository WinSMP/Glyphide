// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.{MockBukkit, ServerMock}

import io.papermc.paper.event.player.AsyncChatEvent

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.{NamedTextColor, TextColor}
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}
import org.junit.jupiter.api.Assertions._

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

import java.util.Collections
import java.util.List as JavaList

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

    /** Writes config keys to disk and reloads, which is what the plugin's own reload path does.
      *
      * @param keys
      *   config path to value; token lists must be Java lists, since `getStringList` ignores anything
      *   else
      */
    private def writeConfig(keys: (String, Any)*): Unit = {
        keys.foreach((path, value) => plugin.getConfig.set(path, value))
        plugin.saveConfig()
        plugin.reloadConfig()
    }

    private def registerListener(settingsHolder: SettingsHolder): Unit =
        server.getPluginManager.registerEvents(ChatListener(settingsHolder), plugin)

    /** Fires a real AsyncChatEvent through the mock server, returns the rendered output.
      *
      * Builds a fresh SettingsHolder and ChatListener per call because both snapshot config values at
      * construction, so they must be created after the test sets its keys.
      */
    private def renderMessage(message: Component): Component = {
        registerListener(new SettingsHolder(plugin))
        fireMessage(message)
    }

    /** Renders one message through the listener that is already registered. */
    private def fireMessage(message: Component): Component = {
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

    private def plainText(component: Component): String =
        PlainTextComponentSerializer.plainText().serialize(component)

    // The final chat component nests $message replacements, so search recursively.
    private def findComponent(root: Component, pred: Component => Boolean): Option[Component] = {
        if (pred(root)) Some(root)
        else root.children().asScala.flatMap(child => findComponent(child, pred)).headOption
    }

    @Test
    def testBasicChatFormatting(): Unit = {
        writeConfig("chat.format" -> "$prefix $username > $message")

        val rendered = renderMessage(Component.text("Hello"))

        assertTrue(plainText(rendered).contains("TestPlayer > Hello"))
    }

    /** A color change inside an active format code must not leak closing tags into the chat.
      *
      * Legacy codes in the chat format are converted to MiniMessage tags; a conversion that
      * closed the previous color while a format tag was still open produced markup that
      * MiniMessage renders as literal text, so players saw stray tags in their chat.
      */
    @Test
    def testLegacyCodesDoNotLeakClosingTags(): Unit = {
        writeConfig("chat.format" -> "&8[&3&nOwner&8]&r $username > $message")

        val rendered = renderMessage(Component.text("Hello"))

        val output = plainText(rendered)
        assertFalse(output.contains("</"))
        assertTrue(output.contains("Owner"))
        assertTrue(output.contains("TestPlayer > Hello"))
    }

    /** The plugin's own emoticon markup must render for every player.
      *
      * Players who may not use MiniMessage themselves still get the plugin's placeholders
      * rendered, because that markup is plugin-controlled rather than player-controlled.
      */
    @Test
    def testHypixelPlaceholderRendersForNonAdmin(): Unit = {
        writeConfig("use-hypixel-placeholders" -> true)

        val rendered = renderMessage(Component.text("i love rust btw <3"))

        val output = plainText(rendered)
        assertTrue(output.contains("❤"))
        assertFalse(output.contains("<red>"))

        val heart = findComponent(rendered, c => plainText(c) == "❤")
        assertTrue(heart.isDefined)
        assertEquals(NamedTextColor.RED, heart.get.color())
    }

    /** The same placeholder must render for a player who may use MiniMessage themselves. */
    @Test
    def testHypixelPlaceholderRendersForAdmin(): Unit = {
        writeConfig("use-hypixel-placeholders" -> true)
        player.addAttachment(plugin, "glyphide.admin", true)

        val rendered = renderMessage(Component.text("i love rust btw <3"))

        val output = plainText(rendered)
        assertTrue(output.contains("❤"))
        assertFalse(output.contains("<red>"))

        val heart = findComponent(rendered, c => plainText(c) == "❤")
        assertTrue(heart.isDefined)
        assertEquals(NamedTextColor.RED, heart.get.color())
    }

    /** The shipped config must leave placeholders off, or every server would substitute them. */
    @Test
    def testPlaceholdersAreOffByDefault(): Unit = {
        val rendered = renderMessage(Component.text("Check [item] and [hotbar]"))

        val output = plainText(rendered)
        assertTrue(output.contains("[item]"))
        assertTrue(output.contains("[hotbar]"))
    }

    /** An id no handler implements must be ignored rather than break chat. */
    @Test
    def testUnknownPlaceholderIsIgnored(): Unit = {
        writeConfig("placeholder.notimplemented.enabled" -> true)

        val rendered = renderMessage(Component.text("Check [item]"))

        assertTrue(plainText(rendered).contains("[item]"))
    }

    // NOTE: MockBukkit 4.115.0 has not implemented item hover serialization. The assertions below
    // never run as of writing but validate the path once upstream implements it.
    @Test
    def testItemPlaceholder(): Unit = {
        writeConfig(
            "placeholder.item.enabled" -> true,
            "placeholder.item.token" -> JavaList.of("[item]")
        )

        val diamondSword = ItemStack.of(Material.DIAMOND_SWORD)
        player.getInventory.setItemInMainHand(diamondSword)

        val rendered = renderMessage(Component.text("Check [item]"))

        assertTrue(plainText(rendered).contains("Diamond Sword"))
        assertFalse(plainText(rendered).contains("[item]"))

        val itemPart = findComponent(
            rendered,
            c => c.hoverEvent() != null && c.hoverEvent().action() == HoverEvent.Action.SHOW_ITEM
        )
        assertTrue(itemPart.isDefined)
    }

    /** The shipped config.yml is registered as Bukkit defaults, so it supplies the token list whenever a
      * server owner enables a placeholder without writing one.
      */
    @Test
    def testShippedTokensApplyWhenConfigHasNone(): Unit = {
        writeConfig("placeholder.item.enabled" -> true)
        assertEquals(List(List("[item]", "[i]")), resolveTokens(plugin.getConfig, "item"))
    }

    /** Without the shipped defaults behind it, a section that lists no tokens falls back to the
      * handler's own list, and a section that lists tokens overrides it.
      *
      * Uses a bare YamlConfiguration because `plugin.getConfig` always carries the packaged defaults.
      */
    @Test
    def testHandlerTokensAreTheFallback(): Unit = {
        val bare = new YamlConfiguration()
        bare.set("placeholder.item.enabled", true)
        assertEquals(List(List("[item]", "[i]")), resolveTokens(bare, "item"))

        bare.set("placeholder.item.token", JavaList.of("[held]", "[hand]"))
        assertEquals(List(List("[held]", "[hand]")), resolveTokens(bare, "item"))
    }

    private def resolveTokens(config: FileConfiguration, id: String): List[List[String]] =
        PlaceholderRegistry
            .resolve(Configuration(config), plugin.getLogger)
            .filter(_.handler.id == id)
            .map(_.tokens)

    /** A reload must swap the settings a message is rendered with, not just the file on disk.
      *
      * Asserted through `chat.format` because rendering an item placeholder needs MockBukkit's
      * unimplemented item hover serialization; the resolved token list is asserted separately above.
      */
    @Test
    def testReloadAppliesToTheNextMessage(): Unit = {
        writeConfig("chat.format" -> "$username before: $message")

        val settingsHolder = new SettingsHolder(plugin)
        registerListener(settingsHolder)
        assertTrue(plainText(fireMessage(Component.text("hi"))).contains("before:"))

        writeConfig("chat.format" -> "$username after: $message")
        settingsHolder.reload()

        assertTrue(plainText(fireMessage(Component.text("hi"))).contains("after:"))
    }

    @Test
    def testUrlHighlighting(): Unit = {
        // top-level keys, no `chat.` prefix
        writeConfig("url.color" -> "#FF0000", "url.hover" -> true)

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
        assertFalse(plainText(rendered).contains("<red>"))
        val messagePart = findComponent(
            rendered,
            c => PlainTextComponentSerializer.plainText().serialize(c) == "Admin message"
        )
        assertTrue(messagePart.isDefined)
        assertEquals(TextColor.fromHexString("#FF5555"), messagePart.get.color())
    }
}
