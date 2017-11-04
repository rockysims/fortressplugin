package me.newyith.util

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

abstract class BaseUUID {
	@JsonProperty("uuid") val uuid: UUID = UUID.randomUUID()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || other !is BaseUUID) return false

		return this.uuid == other.uuid
	}

	override fun hashCode(): Int {
		return uuid.hashCode()
	}
}
