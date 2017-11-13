package me.newyith.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.google.common.base.Splitter
import org.bukkit.Chunk

class ChunkPos(
	@JsonProperty("x") val x: Int,
	@JsonProperty("z") val z: Int
) {
	constructor(chunk: Chunk) : this(chunk.x, chunk.z)

	@JsonValue
	private fun toStringValue(): String {
		val s = StringBuilder()
		s.append(x)
		s.append(",")
		s.append(z)
		return s.toString()
	}

	@JsonCreator
	constructor(s: String) : this(s.split(",")[0].toInt(), s.split(",")[1].toInt())

	override fun toString(): String {
		val s = StringBuilder()
		s.append(x)
		s.append(", ")
		s.append(z)
		return s.toString()
	}

	override fun equals(other: Any?): Boolean {
		if (other === this) return true

		return if (other is ChunkPos) {
			x == other.x && z == other.z
		} else {
			false
		}
	}

	override fun hashCode(): Int {
		var hash = x
		hash = 49999 * hash + z
		return hash
	}
}