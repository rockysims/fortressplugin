package me.newyith.fortress.event

import me.newyith.fortress.main.FortressPlugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class TickTimer(plugin: JavaPlugin) : BukkitRunnable() {
	init {
		this.runTaskTimer(plugin, 0, (msPerTick / 50).toLong())
	}

	override fun run() {
		//TODO: finish writing

		//Log.start("tick");

		//TODO: maybe call commented out parts from within FortressPlugin.onTick() method? not sure
//		FortressesManager.onTick()
//		TimedBedrockManager.onTick()
//		BedrockManager.onTick()
//		Commands.onTick()

		FortressPlugin.onTick()
		//Log.end("tick");
	}

	companion object {
		val msPerTick = 150 //should be divisible by 50
		//tick internally only every 3rd game tick in hopes it will improve performance
	}
}