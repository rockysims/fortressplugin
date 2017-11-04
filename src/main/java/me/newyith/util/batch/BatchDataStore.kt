package me.newyith.util.batch

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*


//use pattern below for other managers too (such as BedrockManager, BatchDataStore)
//TODO: use pattern ProtectionManager.forWorld(world).forCoreId(coreId)
//	then call ProtectionManager.forWorld(world).forCoreId(coreId).save/load() in CoreAnimator
//		that way the core can call save() but each ProtectionManagerForCoreId can be saved separately
//			save in '{world}/{coreId}/protectionManager.json' maybe?
// Note: ProtectionManager.forWorld(world) gives a place for .isProtected() check by world
// Note: in classes using ProtectionManager.forWorld(world).forCoreId(coreId) save instance of it in class (for +performance?)


//TODO: store batch data in Batch and store batches in BaseCore
//	should be able to store reference to Batch in other places
//		maybe BatchDataManager that gets rebuilt from cores onLoad (instead of storing data itself)?
//			not sure yet how I'll want to use batches in other places
//			but BatchDataManager should allow looking up batches by id and thus having the batchId provides a references to it essentially

//SKIP: add saving and loading of groups using SaveLoadManager?
//	need to be able to save a single group
//	ideally allow un/loading groups to save memory (when no fortress chunks are loaded can unload it's batches)
object BatchDataStore {
	@JsonProperty("batchDataStoreByGroup") private val batchDataStoreByGroup: HashMap<BatchGroupUUID, BatchDataStoreForGroup> = HashMap()

	fun forGroup(groupUuid: BatchGroupUUID): BatchDataStoreForGroup {
		return batchDataStoreByGroup.getOrPut(groupUuid, {
			BatchDataStoreForGroup(groupUuid)
		})
	}

	//removeGroup()
	//saveAll()
	//loadAll()






	fun loadGroup(groupUuid: BatchGroupUUID): Boolean {
		//TODO: write
		return false //failed to load
	}

	fun unloadGroup(groupUuid: BatchGroupUUID): Boolean {
		//TODO: write
		return false //failed to unload
	}
}
