package me.newyith.fortress.main

import me.newyith.fortress.event.EventListener
import me.newyith.fortress.event.TickTimer
import me.newyith.util.Log
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class FortressPluginForWorlds {
	val pluginByWorld = HashMap<String, FortressPluginForWorld>()
	private var eventListener: EventListener? = null
	private var tickTimer: TickTimer? = null

	//NOTE: stopped part way into filling out this class
	//	use eventListener, tickTimer
	//	write on enable/disable
	//	look at old FortressPlugin and make sure it all goes into either new FortressPlugin or this class

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginByWorld.getOrPut(world.name, {
			FortressPluginForWorld(world.name)
		})
	}

	fun onEnable(javaPlugin: JavaPlugin) {
		//
	}

	fun onDisable() {
		//
	}

	fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		Log.warn("//TODO: handle command: " + cmd.name)
		return false
	}

	fun onTick() {
		Log.warn("//TODO: handle onTick")
	}
}