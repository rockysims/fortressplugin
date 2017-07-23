package me.newyith.fortress.protection

import org.bukkit.World

/**
 * Provides an easy way to get ProtectionManagerForWorld by world.
 * Transient. Does not load/save data onEnable/Disable.
 * Cleaned. Needs to clean up onDisable.
 */
object ProtectionManager {
	val managerByWorld = HashMap<String, ProtectionManagerForWorld>()

	fun enable() {}

	fun disable() {
		managerByWorld.clear()
	}

	fun forWorld(world: World): ProtectionManagerForWorld {
		return managerByWorld.getOrPut(world.name, {
			ProtectionManagerForWorld(world)
		})
	}
}