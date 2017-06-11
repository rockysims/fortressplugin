package me.newyith.fortress.event

import me.newyith.fortress.main.FortressPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.plugin.java.JavaPlugin

class EventListener(plugin: JavaPlugin) : Listener {
	init {
		plugin.server.pluginManager.registerEvents(this, plugin)
	}

	// --- //

	//ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" at the top method
	@EventHandler(ignoreCancelled = true)
	fun onSignChange(event: SignChangeEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onSignChange(event.player, block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockBreakEvent(event: BlockBreakEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onBlockBreakEvent(event.player, block)
		if (cancel) event.isCancelled = true
	}
}
