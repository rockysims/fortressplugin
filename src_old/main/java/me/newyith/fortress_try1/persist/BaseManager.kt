package me.newyith.fortress_try1.persist

import org.bukkit.World

class BaseManager<ManagerForWorldType>(fileName: String) {
	val managerByWorld = HashMap<String, ManagerForWorldType>()

	fun forWorld(world: World): ManagerForWorldType {
		return managerByWorld.getOrPut(world.name, {

			//if (canLoad) then load from json
			//else create new


			//TODO: try to load from json here before resorting to creating a new manager?

			ProtectionManagerForWorld(world)
		})
	}

	fun forCoreId(coreId: CoreId): ProtectionManagerForCoreId {
		val managerForWorld = forWorld(coreId.world)

		return managerForWorld.forCoreId(coreId, {
			ProtectionManagerForCoreId(coreId)
		})
	}
}