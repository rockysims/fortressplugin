package me.newyith.fortress.main

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.lookup.Lookup
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRuneId
import me.newyith.util.ChunkPos
import me.newyith.util.Log
import me.newyith.util.extension.chunk.getPos
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
	@JsonProperty("generatorRuneIdsByChunkPos") val generatorRuneIdsByChunkPos = HashMap<ChunkPos, Set<GeneratorRuneId>>()
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
		return generatorRuneIdsByChunkPos[chunk.getPos()] ?: HashSet()
	}

	//---//

	fun onTick() {
		loadedGeneratorRuneById.values.forEach { it.onTick() }
	}

	fun onSignChange(player: Player, signBlock: Block): Boolean {
		var cancel = false

		player.sendMessage("onSignChanged() called") //TODO: delete this line

		//TODO: uncomment out block and continue double checking it
//		val runePattern = GeneratorRunePatterns.tryReadyPattern(signBlock)
//		runePattern?.let { runePattern ->
//			val signPoint = Point(signBlock)
//			val runeAlreadyCreated = signPoint in signPoint.getOwnerGeneratorRune(signBlock.world)?.patternPoints ?: HashSet()
//			if (!runeAlreadyCreated) {
//				val generatorRune = GeneratorRune(runePattern)
//				loadedGeneratorRuneById.put(generatorRune.id, generatorRune)
//
//				generatorRune.onCreated(player)
//				cancel = true //otherwise initial text on sign is replaced by what user wrote
//			} else {
//				player.sendMessage(ChatColor.AQUA.toString() + "Failed to create rune because rune already created here.")
//			}
//		}

		return cancel
	}

//	fun onSignChange(player: Player, block: Block): Boolean {
//		val cancel = false
//
//		//TODO: write
//
//		return cancel
//	}
}
