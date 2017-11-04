package me.newyith.fortress.main_

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class FortressPluginWrapper : JavaPlugin() {
	override fun onEnable() {
//		val fp = SaveLoad.load("path/to/jsonFile") as FortressPlugin
//		fp.enable(this)



		FortressPlugin.enable(this)
	}

	override fun onDisable() {
		FortressPlugin.disable()
	}

	override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		return FortressPlugin.onCommand(sender, cmd, label, args)
	}
}