package me.newyith.fortress.lookup

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.Point

class Lookup(
	@JsonProperty("worldName") val worldName: String
) {
	@JsonProperty("protectedPoints") val protectedPoints = HashSet<Point>() //TODO: keep updated
//	@JsonProperty("generatorRuneIdByClaimedWallPoint") val generatorRuneIdByClaimedWallPoint = HashMap<Point, GeneratorRuneId>() //TODO: keep updated
//	etc.
}