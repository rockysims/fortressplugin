package me.newyith.fortress.main_

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.event.EventListener
import me.newyith.util.Log
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class FortressPluginNew {
	@JsonProperty("worldNames") val worldNames = HashSet<String>()
	val pluginByWorld = HashMap<String, FortressPluginForWorld>()

	fun enable() {

	}

	fun disable() {

	}


	companion object {
		val releaseBuild: Boolean = false //TODO: change this to true for release builds
		var realInstance: FortressPluginNew? = null
		var enabled = false

		val instance: FortressPluginNew
			get() {
				if (enabled) throw Exception("Tried to get FortressPluginNew after it was enabled.")
				if (realInstance == null) {
					//load else create
					realInstance = FortressPluginNew()
				}
				return realInstance
			}

//		val instance: FortressPluginNew by lazy {
//			if (enabled) throw Exception("Tried to get FortressPluginNew after it was enabled.")
//			FortressPluginNew()
//		}

		fun onEnable(javaPlugin: JavaPlugin) {
			enabled = true
			//ensure instance is loaded
			instance
		}

		fun onDisable() {
			enabled = false
			//TODO: save realInstance

			realInstance = null
		}

		fun get(): FortressPluginNew {
			if (instance == null)
			return instance
		}
	}






	private var config: ConfigData? = null
	private var plugin: JavaPlugin? = null
	private var saveLoadManager: SaveLoadManager? = null
	private var eventListener: EventListener? = null
	private var tickTimer: TickTimer? = null

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

	fun enable_(javaPlugin: JavaPlugin) {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		config = ConfigManager.loadOrSave(javaPlugin)
		plugin = javaPlugin

		//load pluginByWorld
		pluginByWorld.clear()
		pluginByWorld.putAll(getSaveLoadManager().createPluginByWorld())
		pluginByWorld.values.forEach { it.load() }

		//TODO: consider rewriting these 2 using much the same pattern as ProtectionManager (except use companion object)
		eventListener = EventListener(javaPlugin)
		tickTimer = TickTimer(javaPlugin)

		ProtectionManager.enable()

//		ManualCraftManager.onEnable(this)
//		PearlGlitchFix.onEnable(this)

		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun disable_() {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		ProtectionManager.disable()

		eventListener = null
		tickTimer = null
		saveLoadManager = null

		//save pluginByWorld
		//no need to actually save pluginByWorld because if {worldName} folder exists that means create pluginForWorld
		pluginByWorld.values.forEach { it.save() }
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

	fun onTick() {
		pluginByWorld.values.forEach { it.onTick() }
	}
}