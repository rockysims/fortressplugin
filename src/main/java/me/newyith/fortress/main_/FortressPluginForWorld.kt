package me.newyith.fortress.main_

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.lookup.Lookup
import me.newyith.fortress.persist.GeneratorRunePersistance
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.ChunkAnchor
import me.newyith.util.extension.chunk.getAnchor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World

class FortressPluginForWorld(
	@JsonProperty("worldName") val worldName: String
) {
	@JsonProperty("generatorRuneIdsByChunkAnchor") val generatorRuneIdsByChunkAnchor = HashMap<ChunkAnchor, Set<GeneratorRuneId>>()
	@JsonProperty("lookup") val lookup = Lookup(worldName) //TODO: I don't think this needs to be saved. test it later

	val world: World by lazy {
		Bukkit.getWorld(worldName)
	}

	fun onChunkLoad(chunk: Chunk) {
		//ensure generatorRunes in chunk are loaded
		idsByChunk(chunk).forEach {
			GeneratorRunePersistance.getOrLoad(it)
		}
	}

	fun onChunkUnload(chunk: Chunk) {
		//consider unloading generatorRunes in chunk
		idsByChunk(chunk).stream()
			.map { GeneratorRunePersistance.getOrLoad(it) }
			.filter { it != null }
			.forEach { generatorRune ->
				//consider unloading generatorRune
				val anyChunksLoaded = generatorRune.chunks.any { it.isLoaded }
				if (!anyChunksLoaded) {
					GeneratorRunePersistance.unload(generatorRune.id)
				}
			}
	}

	fun idsByChunk(chunk: Chunk): Set<GeneratorRuneId> {
		return generatorRuneIdsByChunkAnchor[chunk.getAnchor()] ?: HashSet()
	}
}
