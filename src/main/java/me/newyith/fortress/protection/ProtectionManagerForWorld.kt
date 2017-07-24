package me.newyith.fortress.protection

import me.newyith.fortress.extension.block.isProtected
import me.newyith.fortress.main.FortressPlugin
import me.newyith.fortress.rune.generator.GeneratorRune
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.BlockIterator
import java.util.HashMap
import java.util.HashSet

/**
 * Handles enforcement of protected points.
 * Transient. Does not load/save data onEnable/Disable.
 */
class ProtectionManagerForWorld(val world: World) {
	fun isProtected(p: Point): Boolean {
		//TODO: write
		return false
	}

	fun onPistonEvent(movedBlocks: Set<Block>): Boolean {
		return movedBlocks.any { isProtected(Point(it)) }
	}

	fun onExplode(explodeBlocks: Set<Block>, loc: Location): Set<Block> {
		val explodeBlocksToExclude = HashSet<Block>()
		val world = loc.world

		val anyProtectedBlockExploded = explodeBlocks.any { it.isProtected() }
		if (anyProtectedBlockExploded) {
			val allPointsToShield = HashSet<Point>()
			val explosionOrigin = loc.toVector()
			explodeBlocks
				.map { Point(it) }
				.forEach { explodePoint ->
					//if (protected block between explosion and explodePoint)
					//	add protected block to allPointsToShield
					//	remove explodePoint from explodeBlocks
					val direction = explodePoint.add(0.5, 0.5, 0.5).toVector().subtract(explosionOrigin)
					val distance = Math.max(1, explosionOrigin.distance(explodePoint.toVector()).toInt())
					val rayBlocks = BlockIterator(world, explosionOrigin, direction, 0.0, distance)
					var foundShieldInRay = false
					for (rayBlock in rayBlocks) {
						val rayPoint = Point(rayBlock)

						//add first protected rayPoint to allPointsToShield
						if (!foundShieldInRay && isProtected(rayPoint)) {
							allPointsToShield.add(rayPoint)
							foundShieldInRay = true
						}

						//exclude from explosion all points past (and including) first protected rayPoint
						if (foundShieldInRay) explodeBlocksToExclude.add(rayBlock)
					}
				}

			//exclude protected blocks from explosion
			explodeBlocks.filterTo(explodeBlocksToExclude) { it.isProtected() }

			//show shield bedrock
			val pointsToShieldByRune = HashMap<GeneratorRune, MutableSet<Point>>()
			for (pointToShield in allPointsToShield) {
				val rune = FortressPlugin.forWorld(world).getGeneratorRuneByClaimedWallPoint(pointToShield)
				if (rune != null) { //should always be true in theory
					pointsToShieldByRune
						.getOrPut(rune, { HashSet<Point>() })
						.add(pointToShield)
				} else {
					Log.warn("onExplode() failed to show shield because rune == null at " + pointToShield)
				}

				pointsToShieldByRune.forEach { (rune, pointsToShield) -> rune.generatorCore.shield(pointsToShield) }
			}
		}

		return explodeBlocksToExclude
	}

	fun onIgnite(b: Block): Boolean {
		var cancel = false

		val p = Point(b)
		if (isProtected(p)) {
			FortressPlugin.forWorld(world).getGeneratorRuneByClaimedWallPoint(p)?.let {
				it.generatorCore.shield(p)
			}

			cancel = true
		}

		return cancel
	}

	fun onBurn(b: Block): Boolean {
		var cancel = false

		val p = Point(b)
		if (isProtected(p)) {
			FortressPlugin.forWorld(world).getGeneratorRuneByClaimedWallPoint(p)?.let {
				it.generatorCore.shield(p)
			}

			cancel = true
		}

		return cancel
	}





}