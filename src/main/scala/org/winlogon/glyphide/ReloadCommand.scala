// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import io.papermc.paper.command.brigadier.{BasicCommand, CommandSourceStack}

import org.bukkit.entity.Player

class ReloadCommand(settingsHolder: SettingsHolder) extends BasicCommand {

    override def execute(commandSourceStack: CommandSourceStack, args: Array[String]): Unit = {
        val sender = commandSourceStack.getSender
        sender match {
            case player: Player =>
                settingsHolder.reload()

                player.sendRichMessage(
                    "<gray>Glyphide configuration <dark_aqua>reloaded</dark_aqua>.</gray>"
                )
            case _ =>
                sender.sendRichMessage("<red>Only players can execute this command.</red>")
        }
    }

    override def permission(): String = "glyphide.admin.reload"
}
