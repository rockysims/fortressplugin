package me.newyith.fortress.main

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.lookup.Lookup
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.ChunkAnchor
import me.newyith.util.Log
import me.newyith.util.extension.chunk.getAnchor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player

//TODO: continue writing FortressPluginForWorld
//	make list of missing pieces?

class FortressPluginForWorld(
	@JsonProperty("worldName") val worldName: String
) {
	@JsonProperty("generatorRuneIdsByChunkAnchor") val generatorRuneIdsByChunkAnchor = HashMap<ChunkAnchor, Set<GeneratorRuneId>>()
	val loadedGeneratorRuneById = HashMap<GeneratorRuneId, GeneratorRune>()
	val lookup = Lookup(worldName) //TODO: I don't think this needs to be saved. decide later

	val world: World by lazy {
		Bukkit.getWorld(worldName)
	}

	fun onEnable() {}
	fun onDisable() {}

	fun save(generatorRune: GeneratorRune) {
		FortressPlugin.saveLoad.save(generatorRune, generatorRune.id.savePath)
	}

	private fun load(generatorRuneId: GeneratorRuneId): GeneratorRune? {
		val generatorRune: GeneratorRune? = FortressPlugin.saveLoad.load(generatorRuneId.savePath)
		if (generatorRune == null) Log.warn("Failed to load path: " + generatorRuneId.savePath)
		return generatorRune
	}

	fun onChunkLoad(chunk: Chunk) {
		//ensure generatorRunes in chunk are loaded
		idsByChunk(chunk).forEach { generatorRuneId ->
			load(generatorRuneId)?.let { generatorRune ->
				loadedGeneratorRuneById.put(generatorRuneId, generatorRune)
			}
		}
	}

	fun onChunkUnload(chunk: Chunk) {
		//consider unloading generatorRunes in chunk
		idsByChunk(chunk).stream()
			.map { load(it) }
			.forEach {
				it?.let { generatorRune ->
					//consider unloading generatorRune
					val anyChunksLoaded = generatorRune.chunks.any { it.isLoaded }
					if (!anyChunksLoaded) {
						save(generatorRune)
						loadedGeneratorRuneById.remove(generatorRune.id)
					}
				}
			}
	}

	private fun idsByChunk(chunk: Chunk): Set<GeneratorRuneId> {
		return generatorRuneIdsByChunkAnchor[chunk.getAnchor()] ?: HashSet()
	}

	//---//

	fun onTick() {
		//TODO: write
	}

	fun onSignChange(player: Player, block: Block): Boolean {
		val cancel = false

		//TODO: write

		return cancel
	}
}
