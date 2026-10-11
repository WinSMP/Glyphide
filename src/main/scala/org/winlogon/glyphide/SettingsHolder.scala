// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.bukkit.plugin.java.JavaPlugin

import java.util.concurrent.atomic.AtomicReference

/** Holds the current [[Settings]] and swaps them on `/ghreload`.
  *
  * `Configuration` snapshots values when it is constructed, so config is only ever read once per
  * reload here; the listener and the command share this instance, which is what makes a reload
  * apply to both without either of them reading config themselves.
  */
final class SettingsHolder(plugin: JavaPlugin) {
    private val current: AtomicReference[Settings] =
        new AtomicReference(Settings.load(plugin.getConfig, plugin.getLogger))

    /** The settings to render with; read once per chat message, never mid-message. */
    def settings: Settings = current.get()

    def reload(): Unit = {
        plugin.reloadConfig()
        // Bukkit swallows malformed YAML and hands back a blank config, so a broken edit disables
        // everything silently; the placeholder count that PlaceholderRegistry logs is the visible
        // sign
        current.set(Settings.load(plugin.getConfig, plugin.getLogger))
    }
}
