package me.newyith.fortress.main

import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.block.BlockBreakEvent

class FortressPluginForWorld(val worldName: String) {
	val world: World? = Bukkit.getWorld(worldName)

	fun enable() {
		Log.log(worldName + " enable() called")
		//TODO: load
	}

	fun disable() {
		Log.log(worldName + " disable() called")
		//TODO: save
	}

	fun  onBlockBreakEvent(event: BlockBreakEvent) {
		Log.success("BlockBreakEvent at " + Point(event.block))
	}

}