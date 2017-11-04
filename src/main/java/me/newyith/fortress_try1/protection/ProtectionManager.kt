package me.newyith.fortress_try1.protection

import me.newyith.fortress_try1.core.CoreId
import org.bukkit.Bukkit
import org.bukkit.World

/**
 * Provides an easy way to get ProtectionManagerForWorld by world.
 * And a shortcut to get ProtectionManagerForCoreId by coreId.
 * Transient. Does not load/save data onEnable/Disable. //not sure about this part
 * Cleaned. Needs to clean up onDisable.
 */
object ProtectionManager {
	val managerByWorld = HashMap<String, ProtectionManagerForWorld>()

	fun enable() {}

	fun disable() {
		managerByWorld.clear()
		val persistant = 1
		val persistent = 2

	}

	fun forWorld(world: World): ProtectionManagerForWorld {
		return managerByWorld.getOrPut(world.name, {
			//TODO: try to load from json here before resorting to creating a new manager?
			//	do same in ProtectionManagerForWorld::forCoreId() too?
			ProtectionManagerForWorld(world)
		})
	}

	fun forCoreId(coreId: CoreId): ProtectionManagerForCoreId {
		val world = Bukkit.getWorld(coreId.worldName)
		val managerForWorld = forWorld(world)

		return managerForWorld.forCoreId(coreId, {
			ProtectionManagerForCoreId(coreId)
		})
	}
}