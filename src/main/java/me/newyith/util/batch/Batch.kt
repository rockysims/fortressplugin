package me.newyith.util.batch

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Point
import me.newyith.util.BaseUUID
import me.newyith.util.Log

//Note: BatchGroupUUID is the new AuthToken
abstract class Batch(
	@JsonProperty("groupUuid") groupUuid: BatchGroupUUID
) : BaseUUID() {
	constructor(groupUuid: BatchGroupUUID, points: Set<Point>)
		: this(groupUuid)
	{
		val data = BatchData(groupUuid, points)
		BatchDataStore.forGroup(groupUuid)[uuid] = data
	}

	val batchData: BatchData by lazy {
		val data: BatchData? = BatchDataStore.forGroup(groupUuid)[uuid]
		when (data) {
			null -> {
				Log.error("Failed to find BatchData for groupUuid + uuid pair " + groupUuid + " + " + uuid)
				BatchData(groupUuid, HashSet()) //create fake data to keep things from blowing up more than necessary
			}
			else -> data
		}
	}

	val points: Set<Point>
		get() = batchData.points

	//isInGroup() is the new authorizedBy()
	fun isInGroup(otherGroupUuid: BatchGroupUUID): Boolean {
		return batchData.groupUuid == otherGroupUuid
	}

	operator fun contains(p: Point): Boolean {
		return batchData.points.contains(p)
	}

	fun destroy() {
		BatchDataStore.forGroup(batchData.groupUuid).remove(uuid)
	}
}
























//copy of orig:

//abstract class Batch : BaseUUID {
//	private var model: Model? = null
//
//	val points: Set<Point>
//		get() = model!!.batchData.points
//
//	protected class Model @JsonCreator
//	constructor(@JsonProperty("uuid") uuid: UUID) : BaseUUID.Model(uuid) {
//		@Transient private val batchData: BatchData
//
//		init {
//
//			//rebuild transient fields
//			this.batchData = BatchDataStore.get(uuid)
//		}
//	}
//
//	@JsonCreator
//	constructor(@JsonProperty("model") model: Model) : super(model) {
//		this.model = model
//	}
//
//	constructor(authToken: AuthToken, points: Set<Point>) {
//		BatchDataStore.put(super.getUuid(), BatchData(authToken, points))
//		model = Model(super.getUuid())
//	}
//
//	//-----------------------------------------------------------------------
//
//	fun authorizedBy(otherAuthToken: AuthToken): Boolean {
//		return model!!.batchData.authToken == otherAuthToken
//	}
//
//	operator fun contains(p: Point): Boolean {
//		return model!!.batchData.points.contains(p)
//	}
//
//	fun destroy() {
//		BatchDataStore.remove(super.getUuid())
//	}
//}
