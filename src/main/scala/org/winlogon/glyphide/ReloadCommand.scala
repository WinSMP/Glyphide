package org.winlogon.glyphide

import io.papermc.paper.command.brigadier.{BasicCommand, CommandSourceStack}

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ReloadCommand(plugin: JavaPlugin) extends BasicCommand {
    private val pluginRef = plugin

    override def execute(commandSourceStack: CommandSourceStack, args: Array[String]): Unit = {
        val sender = commandSourceStack.getSender
        sender match {
            case player: Player =>
                // call reloadConfig on the plugin instance
                pluginRef.reloadConfig()

                player.sendRichMessage("<gray>Glyphide configuration <dark_aqua>reloaded</dark_aqua>.</gray>")
            case _ =>
                sender.sendRichMessage("<red>Only players can execute this command.</red>")
        }
    }

    override def permission(): String = "glyphide.admin.reload"
}
