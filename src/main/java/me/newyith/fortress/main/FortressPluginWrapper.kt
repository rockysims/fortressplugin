package me.newyith.fortress.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/**
 * Wrapper around plugin with no fields
 * so that /reload doesn't cause memory leak.
 */
class FortressPluginWrapper : JavaPlugin() {
	override fun onEnable() {
		FortressPlugin.onEnable(this)
	}

	override fun onDisable() {
		FortressPlugin.onDisable()
	}

	override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		return FortressPlugin.onCommand(sender, cmd, label, args)
	}
}