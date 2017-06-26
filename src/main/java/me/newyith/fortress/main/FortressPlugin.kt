package me.newyith.fortress.main

import me.newyith.fortress.config.ConfigData
import me.newyith.fortress.config.ConfigManager
import me.newyith.fortress.event.EventListener
import me.newyith.fortress.persist.SaveLoadManager
import me.newyith.util.Log
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

object FortressPlugin {
	val releaseBuild: Boolean = false //TODO: change this to true for release builds
	val pluginByWorld = HashMap<String, FortressPluginForWorld>()
	private var config: ConfigData? = null
	private var plugin: JavaPlugin? = null
	private var eventListener: EventListener? = null
	private var saveLoadManager: SaveLoadManager? = null

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginByWorld.getOrPut(world.name, {
			FortressPluginForWorld(world)
		})
	}

	fun getConfig(): ConfigData {
		return config ?: ConfigManager.getDefaults()
	}

	fun getPlugin(): JavaPlugin? {
		return plugin
	}

	fun getSaveLoadManager(): SaveLoadManager {
		val manager = saveLoadManager ?: SaveLoadManager()
		saveLoadManager = manager
		return manager
	}

	// --- //

	fun enable(javaPlugin: JavaPlugin) {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		config = ConfigManager.loadOrSave(javaPlugin)
		plugin = javaPlugin

		//load pluginByWorld
		pluginByWorld.clear()
		pluginByWorld.putAll(getSaveLoadManager().createPluginByWorld())
		pluginByWorld.values.forEach { it.enable() }

		eventListener = EventListener(javaPlugin)
//		TickTimer.onEnable(this)
//		ManualCraftManager.onEnable(this)
//		PearlGlitchFix.onEnable(this)

		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun disable() {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		eventListener = null
		saveLoadManager = null

		//save pluginByWorld
		//no need to actually save pluginByWorld because if {worldName} folder exists that means create pluginForWorld
		pluginByWorld.values.forEach { it.disable() }
		pluginByWorld.clear()

		config = null
		plugin = null

		Log.sendConsole("         >> OFF <<          ", ChatColor.RED)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		Log.warn("//TODO: handle command: " + cmd.name)
		return false
	}
}
