package me.newyith.fortress.main

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.event.EventListener
import me.newyith.fortress.event.TickTimer
import me.newyith.fortress.persist.SaveLoad
import me.newyith.util.Log
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Singleton responsible for serving up FortressPluginForWorld by world.
 * Home of the eventListener and tickTimer singleton instances.
 * Persisted by FortressPlugin (and by self on enable/disable).
 */
class FortressPluginForWorlds {
	private val pluginByWorld = HashMap<String, FortressPluginForWorld>()
	private var eventListener: EventListener? = null
	private var tickTimer: TickTimer? = null

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginByWorld.getOrPut(world.name, {
			FortressPluginForWorld(world.name)
		})
	}

	fun onEnable(javaPlugin: JavaPlugin) {
		//load pluginByWorld
		val worldNames: Set<String> = FortressPlugin.saveLoad.getWorldNames()
		worldNames.forEach { worldName ->
			if (pluginByWorld[worldName] == null) {
				val path = SaveLoad.getSavePathOfFortressPluginForWorld(worldName)
				val plugin: FortressPluginForWorld? = FortressPlugin.saveLoad.load(path)
				if (plugin != null) pluginByWorld.put(worldName, plugin)
				//else world doesn't have a FortressPluginForWorld instance yet (and doesn't need one yet either)
			}
		}

		pluginByWorld.values.forEach { it.onEnable() } //TODO: delete this line if not needed

		eventListener = EventListener(javaPlugin)
		tickTimer = TickTimer(javaPlugin)
//		manualCraftManager = ManualCraftManager(javaPlugin)
//		pearlGlitchFix = PearlGlitchFix(javaPlugin)
	}

	fun onDisable() {
		//TODO: consider not cleaning up since this whole class will be garbage collected onDisable()
		eventListener = null
		tickTimer = null

		//save pluginByWorld
		pluginByWorld.entries.forEach {
			val worldName = it.key
			val plugin = it.value
			val path = SaveLoad.getSavePathOfFortressPluginForWorld(worldName)
			FortressPlugin.saveLoad.save(plugin, path)
		}

		pluginByWorld.values.forEach { it.onDisable() } //TODO: delete this line if not needed
		pluginByWorld.clear()
	}

	fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		var commandHandled = false

		// /stuck
		if (command.name.equals("stuck", ignoreCase = true) && sender is Player) {
			Log.warn("//TODO: handle command: /" + command.name)
//			Commands.onStuckCommand(sender)
			commandHandled = true
		}

		// /fort [subCommand]
		if (command.name.equals("fort", ignoreCase = true) && args.size > 0 && sender is Player) {
			val subCommand = args[0]

			// /fort stuck
			if (subCommand.equals("stuck", ignoreCase = true)) {
				Log.warn("//TODO: handle command: /" + command.name + " " + subCommand)
//				Commands.onStuckCommand(sender)
				commandHandled = true
			}

			if (!commandHandled) {
				val msg = "Unknown command: /" + command.name + " " + subCommand
				sender.sendMessage(ChatColor.AQUA.toString() + msg)
			}
		}

		return commandHandled
	}

	//called by tickTimer
	fun onTick() {
		pluginByWorld.values.forEach { it.onTick() }
	}
}