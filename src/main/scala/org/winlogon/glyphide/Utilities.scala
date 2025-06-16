// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.ChatColor
import org.bukkit.entity.Player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.model.user.User
import net.luckperms.api.{LuckPerms, LuckPermsProvider}

import scala.util.{Try, Success, Failure}

object Formatter {
    enum Permission(val name: String) {
        case Admin extends Permission("glyphide.admin")
    }

    private val basicTagsResolver = TagResolver.builder()
        .resolver(StandardTags.color())
        .resolver(StandardTags.decorations())
        .resolver(StandardTags.gradient())
        .build()

    private val advancedTagsResolver = TagResolver.builder()
        .resolver(StandardTags.clickEvent())
        .resolver(StandardTags.color())
        .resolver(StandardTags.decorations())
        .resolver(StandardTags.defaults())
        .resolver(StandardTags.font())
        .resolver(StandardTags.gradient())
        .resolver(StandardTags.hoverEvent())
        .resolver(StandardTags.insertion())
        .resolver(StandardTags.keybind())
        .resolver(StandardTags.nbt())
        .resolver(StandardTags.newline())
        .resolver(StandardTags.pride())
        .resolver(StandardTags.rainbow())
        .resolver(StandardTags.reset())
        .resolver(StandardTags.score())
        .resolver(StandardTags.selector())
        .resolver(StandardTags.shadowColor())
        .resolver(StandardTags.transition())
        .resolver(StandardTags.translatable())
        .resolver(StandardTags.translatableFallback())
        .build()

    given basicResolver: TagResolver = basicTagsResolver
    given advancedResolver: TagResolver = advancedTagsResolver

    extension (p: Player) {
        private def luckPermsUser: Option[User] = Try(LuckPermsProvider.get())
            .toOption
            .flatMap(lp => Option(lp.getUserManager.getUser(p.getUniqueId)))
    }

    extension (user: User) {
        private def prefix: Option[String] = Option(user.getCachedData.getMetaData.getPrefix)
        private def suffix: Option[String] = Option(user.getCachedData.getMetaData.getSuffix)
    }

    private def deserializeComponent(input: String)(using resolver: TagResolver): Component = {
        MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(input), resolver)
    }

    private def getComponent(player: Player, extractor: User => Option[String])(using resolver: TagResolver): Component = {
        player.luckPermsUser
            .flatMap(extractor)
            .map(deserializeComponent)
            .getOrElse(Component.empty())
    }

    def getPrefix(player: Player)(using resolver: TagResolver): Component =
        getComponent(player, _.prefix)

    def getSuffix(player: Player)(using resolver: TagResolver): Component =
        getComponent(player, _.suffix)

    def formatMessage(player: Player, message: String): Component = {
        player.hasPermission(Permission.Admin.name) match {
            case true => usingAdvancedResolver(message)
            case false => basicFormat(message)
        }
    }

    private def usingAdvancedResolver(message: String): Component = {
        given TagResolver = advancedResolver
        val converted = convertLegacyToMiniMessage(message)
            .replaceAll("\\\\>", ">")
            .replaceAll("\\\\<", "<")

        Try(MiniMessage.builder().build().deserialize(converted, summon[TagResolver])) match {
            case Success(c) => c
            case Failure(_) => LegacyComponentSerializer.legacyAmpersand().deserialize(converted)
        }
    }

    private def basicFormat(message: String): Component = {
        LegacyComponentSerializer.legacyAmpersand().deserialize(
            message
                .replaceAll("(?i)&k", "")
                .replaceAll("(?i)&([0-9a-f])", "&$1")
        )
    }

    def convertLegacyToMiniMessage(input: String): String = {
        val translated = ChatColor.translateAlternateColorCodes('&', input)
        MiniMessage.miniMessage().serialize(LegacyComponentSerializer.legacySection().deserialize(translated))
    }
}
