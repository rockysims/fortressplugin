package me.newyith.fortress.rune.generator

import me.newyith.util.Point
import org.bukkit.Chunk

class GeneratorRune(val id: GeneratorRuneId) {
	//TODO: write

	val chunks: Set<Chunk> = HashSet()

	val patternPoints: Set<Point> = HashSet()
	val claimedWallPoints: Set<Point> = HashSet()
}