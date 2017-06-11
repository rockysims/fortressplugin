package me.newyith.fortress.main

import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.fortress.rune.generator.GeneratorRunePatterns
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.ChatColor
import org.bukkit.World
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
		player.sendMessage(ChatColor.AQUA.toString() + "Test aqua color") //TODO: delete this line
		var cancel = false

		val runePattern = GeneratorRunePatterns.tryReadyPattern(signBlock)
		runePattern?.let { runePattern ->
			val runeAlreadyCreated = Point(signBlock) in generatorRuneByPatternPoint.keys
			if (!runeAlreadyCreated) {
				val rune = GeneratorRune(runePattern)
				generatorRunes.add(rune)

				//add new rune to generatorRuneByPoint map
				runePattern.points.forEach {
					generatorRuneByPatternPoint.put(it, rune)
				}

				rune.onCreated(player)
				cancel = true //otherwise initial text on sign is replaced by what user wrote
			} else {
				player.sendMessage(ChatColor.AQUA.toString() + "Failed to create rune because rune already created here.")
			}
		}

		return cancel
	}

	fun onBlockBreakEvent(player: Player, block: Block): Boolean {
		Log.warn("//TODO: handle onBlockBreakEvent() at " + Point(block) + " by " + player.name)
		return false
	}
}