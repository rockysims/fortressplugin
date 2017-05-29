package me.newyith.fortress.main

import me.newyith.util.Log
import org.bukkit.Bukkit

class FortressPluginForWorld(val worldName :String) {
	val world = Bukkit.getWorld(worldName)

	fun enable() {
		Log.log(worldName + " enable() called")
	}

	fun disable() {
		Log.log(worldName + " disable() called")
	}

}