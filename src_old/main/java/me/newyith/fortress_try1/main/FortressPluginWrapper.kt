package me.newyith.fortress_try1.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class FortressPluginWrapper : JavaPlugin() {
	override fun onEnable() {
		FortressPlugin.enable(this)
	}

	override fun onDisable() {
		FortressPlugin.disable()
	}

	override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		return FortressPlugin.onCommand(sender, cmd, label, args)
	}
}

//moved TODOs into new FortressPluginWrapper (since this one is now try_1)