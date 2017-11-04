package me.newyith.util.batch

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

class BatchDataStoreForGroup(
	@JsonProperty("groupUuid") val groupUuid: BatchGroupUUID
) {
	@JsonProperty("batchDataByUuid") private val batchDataByUuid: HashMap<UUID, BatchData> = HashMap()

	operator fun get(uuid: UUID): BatchData? {
		return batchDataByUuid[uuid]
	}

	operator fun set(uuid: UUID, batchData: BatchData) {
		batchDataByUuid[uuid] = batchData
	}

	fun remove(uuid: UUID) {
		batchDataByUuid.remove(uuid)
	}

	//---//

	//TODO: handle save/load (and always save group when CoreAnimator saves?)

	fun load() {

	}

	fun save() {
		batchDataByUuid.values.forEach { it.save() }
	}
}
