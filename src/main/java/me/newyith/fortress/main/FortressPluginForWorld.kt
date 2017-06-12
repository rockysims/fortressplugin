package me.newyith.fortress.main

import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRunePatterns
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.*

class FortressPluginForWorld(val world: World) {
	private val generatorRunes = HashSet<GeneratorRune>()
	private val generatorRuneByPatternPoint = HashMap<Point, GeneratorRune>()
	private val saveLoadManager = FortressPlugin.getSaveLoadManager()

	fun enable() {
		Log.log(world.name + " FortressPluginForWorld::enable() called") //TODO: delete this line

		//load generatorRunes
		generatorRunes.clear()
		generatorRunes.addAll(saveLoadManager.loadGeneratorRunes(world))

		//rebuild generatorRuneByPatternPoint
		for (generatorRune in generatorRunes) {
			for (patternPoint in generatorRune.pattern.points) {
				generatorRuneByPatternPoint.put(patternPoint, generatorRune)
			}
		}
	}

	fun disable() {
		Log.log(world.name + " FortressPluginForWorld::disable() called") //TODO: delete this line

		//save generatorRunes
		generatorRunes.parallelStream().forEach {
			saveLoadManager.saveGeneratorRune(it)
		}
	}

	fun onSignChange(player: Player, signBlock: Block): Boolean {
		var cancel = false

		val runePattern = GeneratorRunePatterns.tryReadyPattern(signBlock)
		runePattern?.let { runePattern ->
			val runeAlreadyCreated = Point(signBlock) in generatorRuneByPatternPoint.keys
			if (!runeAlreadyCreated) {
				val generatorRune = GeneratorRune(runePattern)
				generatorRunes.add(generatorRune)

				//add new generatorRune to generatorRuneByPoint map
				runePattern.points.forEach {
					generatorRuneByPatternPoint.put(it, generatorRune)
				}

				generatorRune.onCreated(player)
				cancel = true //otherwise initial text on sign is replaced by what user wrote
			} else {
				player.sendMessage(ChatColor.AQUA.toString() + "Failed to create rune because rune already created here.")
			}
		}

		return cancel
	}

	fun onBlockBreakEvent(player: Player, brokenBlock: Block): Boolean {
		var cancel = false

		val inCreative = player.gameMode == GameMode.CREATIVE
		if (!inCreative) {
			/* //TODO: uncomment out this block
			val brokenPoint = Point(brokenBlock)
			if (isProtected(brokenPoint)) { //was isGenerated()
				cancel = true
				//was getRuneByClaimedWallPoint()
				getGeneratorRuneByClaimedWallPoint(brokenPoint)?.let { generatorRune ->
					generatorRune.getGeneratorCore().shield(brokenPoint)
				}
			}
			// */

			//commented out because we're not gonna bother handling piston special case yet (handling it here is not elegant)
//			if (!isProtected) {
//				when (brokenPoint.getType(world)) {
//					Material.PISTON_EXTENSION, Material.PISTON_MOVING_PIECE -> {
//						val matData = brokenBlock.getState().getData()
//						if (matData is PistonExtensionMaterial) {
//							val pem = matData as PistonExtensionMaterial
//							val face = pem.getFacing().getOppositeFace()
//							val pistonBasePoint = Point(brokenBlock.getRelative(face, 1))
//							if (model.protectedPoints.contains(pistonBasePoint)) {
//								cancel = true
//								if (rune != null) rune!!.getGeneratorCore().shield(brokenPoint)
//							}
//						} else {
//							Debug.error("matData not instanceof PistonExtensionMaterial")
//						}
//					}
//				}
//			}
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(brokenBlock)
		}

		return cancel
	}

	private fun onRuneMightHaveBeenBrokenBy(block: Block) {
		generatorRuneByPatternPoint[Point(block)]?.let { breakRune(it) }
	}

	fun breakRune(generatorRune: GeneratorRune) {
		//TODO: write this (or rather translate it)

		generatorRune.onBroken()
		//breaking should naturally update: generatedPoints and generatorRuneByProtectedPoint

		for (p in generatorRune.pattern.points) {
			generatorRuneByPatternPoint.remove(p)
		}

		generatorRunes.remove(generatorRune)
	}
}