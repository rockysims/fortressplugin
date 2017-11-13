package me.newyith.fortress_try1.bedrock

import me.newyith.fortress_try1.core.CoreId
import org.bukkit.Bukkit
import org.bukkit.World


/**
 * Provides an easy way to get BedrockManagerForWorld by world.
 * And a shortcut to get BedrockManagerForCoreId by coreId.
 * Transient. Does not load/save data onEnable/Disable. //not sure about this part
 * Cleaned. Needs to clean up onDisable.
 */
object BedrockManager {
	val managerByWorld = HashMap<String, BedrockManagerForWorld>()

	fun enable() {}

	fun disable() {
		managerByWorld.clear()
	}

	fun forWorld(world: World): BedrockManagerForWorld {
		return managerByWorld.getOrPut(world.name, {
			BedrockManagerForWorld(world)
		})
	}

	fun forCoreId(coreId: CoreId): BedrockManagerForCoreId {
		val world = Bukkit.getWorld(coreId.worldName)
		return forWorld(world).forCoreId(coreId)
	}
}