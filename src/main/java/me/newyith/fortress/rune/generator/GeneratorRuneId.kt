package me.newyith.fortress.rune.generator

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.World

class GeneratorRuneId(
	@JsonProperty("worldName") val worldName: String,
	@JsonProperty("anchorPoint") val anchorPoint: Point
) {
	val world: World by lazy {
		Bukkit.getWorld(worldName)
	}

	override fun toString(): String {
		return worldName + "@" + anchorPoint
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || other !is GeneratorRuneId) return false

		return this.worldName == other.worldName
			&& this.anchorPoint == other.anchorPoint
	}

	override fun hashCode(): Int {
		return toString().hashCode()
	}
}