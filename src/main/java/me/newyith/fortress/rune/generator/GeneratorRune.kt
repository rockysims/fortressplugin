package me.newyith.fortress.rune.generator

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Point
import org.bukkit.Chunk
import org.bukkit.entity.Player

class GeneratorRune(
	@JsonProperty("id") val id: GeneratorRuneId
) {
	//TODO: write

	constructor(pattern: GeneratorRunePattern) : this(GeneratorRuneId(pattern.worldName, pattern.anchorPoint))

	val chunks: Set<Chunk> = HashSet()

	val patternPoints: Set<Point> = HashSet()
	val claimedWallPoints: Set<Point> = HashSet()

	fun onTick() {
		//TODO: write
	}

	fun onCreated(player: Player) {
		//TODO: write
	}
}
