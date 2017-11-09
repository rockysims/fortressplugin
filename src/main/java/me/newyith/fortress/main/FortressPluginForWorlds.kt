package me.newyith.fortress.main

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.event.EventListener
import me.newyith.fortress.event.TickTimer
import me.newyith.fortress.persist.SaveLoad
import me.newyith.util.Log
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Singleton responsible for serving up FortressPluginForWorld by world.
 * Home of the eventListener and tickTimer singleton instances.
 * Persisted by FortressPlugin.
 */
class FortressPluginForWorlds {







	//TODO: make this not a JsonProperty so we can instead save under worlds/ folder?
	//	need to save/load pluginByWorld on disable/enable
	//	also makes discovery of new world folders part of the default loading approach (instead of something tacked on as it is now)
	//	also helps enable SaveLoad::getWorldNames()
	@JsonProperty("pluginByWorld") val pluginByWorld = HashMap<String, FortressPluginForWorld>()











	private var eventListener: EventListener? = null
	private var tickTimer: TickTimer? = null

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginByWorld.getOrPut(world.name, {
			FortressPluginForWorld(world.name)
		})
	}

	fun onEnable(javaPlugin: JavaPlugin) {
		//TODO: once pluginByWorld is not a jackson field, load it here

		//discover any new world data folders //TODO: double check that this works
		val worldNames: Set<String> = FortressPlugin.saveLoad.getWorldNames()
		worldNames.forEach { worldName ->
			if (pluginByWorld[worldName] == null) {
				val path = SaveLoad.getSavePathOfFortressPluginForWorld(worldName)
				val plugin: FortressPluginForWorld? = FortressPlugin.saveLoad.load(path)
				if (plugin != null) pluginByWorld.put(worldName, plugin)
				else Log.warn("Failed to load path: " + path)
			}
		}

		pluginByWorld.forEach { it.onEnable() } //TODO: delete this line if not needed

		eventListener = EventListener(javaPlugin)
		tickTimer = TickTimer(javaPlugin)
//		manualCraftManager = ManualCraftManager(javaPlugin)
//		pearlGlitchFix = PearlGlitchFix(javaPlugin)
	}

	fun onDisable() {
		eventListener = null
		tickTimer = null

		//TODO: once pluginByWorld is not a jackson field, save it here

		pluginByWorld.forEach { it.onDisable() } //TODO: delete this line if not needed
	}

	fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		var commandHandled = false

		// /stuck
		if (command.name.equals("stuck", ignoreCase = true) && sender is Player) {
			Log.warn("//TODO: handle command: " + command.name)
//			Commands.onStuckCommand(sender)
			commandHandled = true
		}

		// /fort [subCommand]
		if (command.name.equals("fort", ignoreCase = true) && args.size > 0 && sender is Player) {
			val subCommand = args[0]

			// /fort stuck
			if (subCommand.equals("stuck", ignoreCase = true)) {
				Log.warn("//TODO: handle command: " + command.name)
//				Commands.onStuckCommand(sender)
				commandHandled = true
			}
		}

		return commandHandled
	}

	//called by tickTimer
	fun onTick() {
		Log.warn("//TODO: handle onTick()")
	}
}