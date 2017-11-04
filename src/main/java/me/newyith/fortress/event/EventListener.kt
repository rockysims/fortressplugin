package me.newyith.fortress.event


import me.newyith.fortress_try1.main.FortressPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.plugin.java.JavaPlugin

class EventListener(plugin: JavaPlugin) : Listener {
	init {
		plugin.server.pluginManager.registerEvents(this, plugin)
	}

	//ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" at top of method
	@EventHandler(ignoreCancelled = true)
	fun onSignChange(event: SignChangeEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onSignChange(event.player, block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onChunkLoad(event: ChunkLoadEvent) {
		FortressPlugin.forWorld(event.world).onChunkLoad(event.chunk)
	}

	@EventHandler(ignoreCancelled = true)
	fun onChunkUnload(event: ChunkUnloadEvent) {
		FortressPlugin.forWorld(event.world).onChunkUnload(event.chunk)
	}
}