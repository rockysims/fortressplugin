package me.newyith.fortress.main

import me.newyith.fortress.config.ConfigData
import me.newyith.fortress.config.ConfigManager
import me.newyith.fortress.util.Log
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

	fun getConfig(): ConfigData? {
		return config
	}

	fun getPlugin(): JavaPlugin? {
		return plugin
	}

	//---



	fun enable(javaPlugin: JavaPlugin) {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		config = ConfigManager.load(javaPlugin)
		plugin = javaPlugin











		//TODO: enable

//		loadConfig(); //done
//
//		saveLoadManager = new SaveLoadManager(this); //handle save/load in FortressPluginForWorld class
//		saveLoadManager.load();
//
//		if (!releaseBuild) {
//			sandboxSaveLoadManager = new SandboxSaveLoadManager(this);
////			sandboxSaveLoadManager.load();
//		}
//
//		EventListener.onEnable(this); //maybe keep these 4 here? maybe move to FortressPluginForWorld class?
//		TickTimer.onEnable(this);
//		ManualCraftManager.onEnable(this);
//		PearlGlitchFix.onEnable(this);








		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun disable() {
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD)

		//TODO: disable
		pluginByWorld.clear()

		this.plugin = null

		Log.sendConsole("         >> OFF <<          ", ChatColor.RED)
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED)
	}

	fun forWorld(world: World): FortressPluginForWorld {
		return pluginByWorld.getOrPut(world.name, {
			FortressPluginForWorld(world.name)
		})
	}






	fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		//TODO: write
		return false
	}
}