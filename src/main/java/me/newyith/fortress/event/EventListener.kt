package me.newyith.fortress.event

import me.newyith.fortress.main.FortressPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.java.JavaPlugin

class EventListener(plugin: JavaPlugin) : Listener {
	init {
		plugin.server.pluginManager.registerEvents(this, plugin)
	}

	// --- //

	//ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" to the method
	@EventHandler(ignoreCancelled = true)
	fun onBlockBreakEvent(event: BlockBreakEvent) {
		FortressPlugin.forWorld(event.block.world).onBlockBreakEvent(event)
	}
}
