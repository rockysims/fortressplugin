package me.newyith.util.batch

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableSet
import me.newyith.util.Point

class BatchData(groupUuid: BatchGroupUUID, points: Set<Point>) {
	@JsonProperty("groupUuid") val groupUuid: BatchGroupUUID = BatchGroupUUID()
	@JsonProperty("points") val points: ImmutableSet<Point> = ImmutableSet.copyOf(points)
}
