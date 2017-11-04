package me.newyith.fortress_try1.persist

import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.World

//new idea (PersistableManager)
abstract class PersistableManager<ManagerForWorldType> {
	@JsonProperty("worldNames") val worldNames = HashSet<String>()
	val managerByWorld = HashMap<String, ManagerForWorldType>()

	fun forWorld(world: World): ManagerForWorldType {

		return managerByWorld.getOrPut(world.name, {
			worldNames.add(world.name)

			//if (canLoad) then load
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