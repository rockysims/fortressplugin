package me.newyith.fortress.persist

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.Log

/**
 * Serves up GeneratorRune by GeneratorRuneId.
 * Loads from file if not already loaded.
 * Also allows saving and unloading by GeneratorRuneId.
 */
//object GeneratorRunePersistance {
//	//TODO: save GeneratorRunePersistance on enable/disable
//	@JsonProperty("generatorRuneById") val generatorRuneById = HashMap<GeneratorRuneId, GeneratorRune>()
//
//	fun getOrLoad(id: GeneratorRuneId): GeneratorRune? {
//		val generatorRune = generatorRuneById[id] ?: load(id)
//		if (generatorRune == null) {
//			Log.error("GeneratorRunePersistance::getOrLoad() failed to get/load GeneratorRune with id: " + id)
//		}
//		return generatorRune
//	}
//
//	fun unload(id: GeneratorRuneId) {
//		val generatorRune = generatorRuneById.remove(id)
//		if (generatorRune == null) {
//			Log.error("GeneratorRunePersistance::unload() failed to find GeneratorRune with id: " + id)
//		}
//	}
//
//	fun save(id: GeneratorRuneId) {
//		//TODO: try to save
//	}
//
//	fun load(id: GeneratorRuneId): GeneratorRune? {
//		val generatorRune = null
//
//		//TODO: try to load generatorRune via saveLoadManager
//		//	also load managers? no, managers are stored inside generatorRune so they will be already loaded
//		//	do need to update forWorld().lookup though
//		//		or will managers do that?
//
//		if (generatorRune == null) {
//			Log.error("GeneratorRunePersistance::getOrLoad() failed to load GeneratorRune with id: " + id)
//		}
//		return generatorRune
//	}
//}




class GeneratorRunePersistance {
	//TODO: save GeneratorRunePersistance on enable/disable
	@JsonProperty("generatorRuneById") val generatorRuneById = HashMap<GeneratorRuneId, GeneratorRune>()

	companion object {
		var instance: GeneratorRunePersistance? = null

		fun onEnable() {
			//try to load instance else create
		}

		fun onDisable() {
			//save instance
			if (instance != null) {

			}
		}

		fun getOrLoad(id: GeneratorRuneId): GeneratorRune? {
			val generatorRune = instance.generatorRuneById[id] ?: load(id)
			if (generatorRune == null) {
				Log.error("GeneratorRunePersistance::getOrLoad() failed to get/load GeneratorRune with id: " + id)
			}
			return generatorRune
		}

		fun unload(id: GeneratorRuneId) {
			val generatorRune = instance.generatorRuneById.remove(id)
			if (generatorRune == null) {
				Log.error("GeneratorRunePersistance::unload() failed to find GeneratorRune with id: " + id)
			}
		}

		fun save(id: GeneratorRuneId) {
			//TODO: try to save
		}

		fun load(id: GeneratorRuneId): GeneratorRune? {
			val generatorRune = null

			//TODO: try to load generatorRune via saveLoadManager
			//	also load managers? no, managers are stored inside generatorRune so they will be already loaded
			//	do need to update forWorld().lookup though
			//		or will managers do that?

			if (generatorRune == null) {
				Log.error("GeneratorRunePersistance::getOrLoad() failed to load GeneratorRune with id: " + id)
			}
			return generatorRune
		}
	}
}