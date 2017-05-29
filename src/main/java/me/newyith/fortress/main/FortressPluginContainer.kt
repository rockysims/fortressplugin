package me.newyith.fortress.main

import me.newyith.util.Log
import org.bukkit.plugin.java.JavaPlugin

class FortressPluginContainer: JavaPlugin() {
	private var plugin: FortressPlugin? = null

	override fun onEnable() {
		plugin = FortressPlugin()
		//plugin.onEnable();
		Log.log("onEnable() called.")
	}

	override fun onDisable() {
		//plugin.onDisable();
		Log.log("onDisable() called.")
		plugin = null
	}

	fun getInstance(): FortressPlugin? {
		return plugin //FIX: plugin doesn't extend JavaPlugin (maybe don't need a FortressPluginContainer instead just use both static and not?)
	}
}