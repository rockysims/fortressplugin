package me.newyith.fortress.extension.point

import me.newyith.fortress.main.FortressPlugin
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.ChunkAnchor
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.World

fun Point.getOwnerGeneratorRune(world: World): GeneratorRune? {
	val chunkAnchor = ChunkAnchor(this.getChunk(world))
	val ids = FortressPlugin.forWorld(world).generatorRuneIdsByChunkAnchor[chunkAnchor] ?: HashSet()
	var foundGeneratorRune: GeneratorRune? = null
	ids.any { id ->
		val generatorRune = id.generatorRune
		if (generatorRune != null) {
			if (this in generatorRune.claimedWallPoints || this in generatorRune.patternPoints) {
				foundGeneratorRune = generatorRune
				true
			}
		} else Log.warn("Failed to find generatorRune by id: " + id)
		false
	}

	return foundGeneratorRune
}