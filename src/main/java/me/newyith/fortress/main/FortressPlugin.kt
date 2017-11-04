package me.newyith.fortress.main

import me.newyith.fortress.config.ConfigData
import me.newyith.fortress.config.ConfigManager
import me.newyith.fortress.persist.SaveLoad
import me.newyith.util.Log
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

//TODO: finish FortressPluginForWorlds pattern even though we're trying to save things at the rune level not global level
//	avoids refactoring later if I seem to need something at the global / world level
//	CONSIDER: ideally by not storing things at global level (reconstruct instead) we allow world data folders to be added/removed without breaking things
//	FortressPluginForWorlds can load all FortressPluginForWorld based on world folders rather than saving list of worldNames
//TODO: consider having a protectionManager, etc. instance for each generatorRune
//			world.isProtected(point) can look up runes by chunk and call isProtected() for each rune <---------------
//	avoids multiple json files per generatorRune
//	avoids more special cases for SaveLoad to know how to save/load
//		which avoids complexity

/**
 * Responsible for saving/loading FortressPluginForWorlds.
 * Also provides centralized access to some globals such as isRelease.
 * Must set all its fields to null onDisable() to avoid memory leak when using /reload command.
 */
object FortressPlugin {
	val releaseBuild get() = false //TODO: change this to true for release builds
	var pluginForWorldsReal: FortressPluginForWorlds? = null
	private var saveLoadReal: SaveLoad? = null
	private var config: ConfigData? = null
	private var plugin: JavaPlugin? = null

	// save & load //

	private val pluginForWorlds get() = getOrLoadOrCreatePluginForWorlds()
	private val saveLoad get() = getOrCreateSaveLoad()

	fun getOrLoadOrCreatePluginForWorlds(): FortressPluginForWorlds {
		val pluginForWorlds = pluginForWorldsReal
			?: saveLoad.load("fortressPluginForWorlds")
			?: FortressPluginForWorlds()
		pluginForWorldsReal = pluginForWorlds
		return pluginForWorlds
	}

	fun savePluginForWorlds() {
		saveLoad.save(pluginForWorlds, "fortressPluginForWorlds")
	}

	fun getOrCreateSaveLoad(): SaveLoad {
		val saveLoad = saveLoadReal ?: SaveLoad()
		saveLoadReal = saveLoad
		return saveLoad
	}

	// getters //

	fun getConfig(): ConfigData {
		return config ?: ConfigManager.getDefaults()
	}

	fun getPlugin(): JavaPlugin? {
		return plugin
	}

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginForWorlds.forWorld(world)
	}

	// handlers //

	fun onEnable(javaPlugin: JavaPlugin) {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		FortressPlugin.config = ConfigManager.loadOrSave(javaPlugin)
		FortressPlugin.plugin = javaPlugin

		//load or create pluginForWorlds
		getOrLoadOrCreatePluginForWorlds() //this line is not strictly necessary
		pluginForWorlds.onEnable(javaPlugin)

		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun onDisable() {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		//save pluginForWorlds
		pluginForWorlds.onDisable()
		savePluginForWorlds()

		FortressPlugin.pluginForWorldsReal = null
		FortressPlugin.saveLoadReal = null
		FortressPlugin.config = null
		FortressPlugin.plugin = null

		Log.sendConsole("         >> OFF <<          ", ChatColor.RED)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		return pluginForWorlds.onCommand(sender, cmd, label, args)
	}

	fun onTick() {
		pluginForWorlds.onTick()
	}
}