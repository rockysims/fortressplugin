package me.newyith.fortress_try1.protection

import me.newyith.fortress_try1.core.CoreId
import me.newyith.fortress_try1.extension.block.isProtected
import me.newyith.fortress_try1.extension.location.enforceMinEdgeDist
import me.newyith.fortress_try1.extension.point.*
import me.newyith.fortress_try1.main.FortressPlugin
import me.newyith.fortress_try1.rune.generator.GeneratorRune
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BlockIterator
import java.util.HashMap
import java.util.HashSet

class ProtectionManagerForWorld(val world: World) {
	val managerByCoreId = HashMap<CoreId, ProtectionManagerForCoreId>()

	fun forCoreId(coreId: CoreId): ProtectionManagerForCoreId {
		val managerForWorld = forWorld(coreId.world)

		return managerForWorld.forCoreId(coreId, {
			ProtectionManagerForCoreId(coreId)
		})
	}


}


class ProtectionManagerForCoreId(val world: World) {










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

	fun onEndermanPickupBlock(block: Block): Boolean {
		return block.isProtected() //cancel if protected
	}

	fun onZombieBreakBlock(block: Block): Boolean {
		return block.isProtected() //cancel if protected
	}


	fun onPlayerExitVehicle(player: Player): Boolean {
		var cancel = false

		//if (player in protected point) stuck teleport away immediately with message
		val world = player.world
		val eyesPoint = Point(player.eyeLocation)
		val feetPoint = eyesPoint.add(0.0, -1.0, 0.0)
		if (eyesPoint.isProtected(world) || feetPoint.isProtected(world)) {
			val teleported = false //StuckPlayer.teleport(player) //TODO: write StuckPlayer.teleport() method
			player.sendMessage(ChatColor.AQUA.toString() + "You got stuck in fortress wall.")
			if (!teleported) {
				player.sendMessage(ChatColor.AQUA.toString() + "Stuck teleport failed because no suitable destination was found.")
				cancel = true //canceling would allow trap minecarts (except /spawn should get you out, right?)
				//				not canceling would allow forced fortress entry (if enemy can scan freely above and below fortress)
			}
		}

		cancel = false //TODO: delete this line

		return cancel
	}

	fun onEntityDamageFromExplosion(damagee: Entity, damager: Entity): Boolean {
		var cancel = false

		//SKIP?: once feet are safe from explosion, do same for eyes? no because this is for all entities which might not be 2 tall
		//	consider doing point to bounding box check (see https://gist.github.com/aadnk/7123926)

		//if (protected block between damagee and damager) cancel
		val world = damagee.world
		val source = Point(damager.location).add(0.0, 0.5, 0.0)
		val target = Point(damagee.location).add(0.0, 0.5, 0.0)
		val direction = target.toVector().subtract(source.toVector())
		val distance = Math.max(1, source.distance(target).toInt())
		val rayBlocks = BlockIterator(world, source.toVector(), direction, 0.0, distance)
		while (rayBlocks.hasNext()) {
			val rayBlock = rayBlocks.next()
			if (rayBlock.isProtected()) {
				cancel = true
				Log.log("cancelled explosion damage due to protected rayPoint " + Point(rayBlock))
//				Log.particleAtTimed(rayPoint, ParticleEffect.HEART)
				break
			} else {
//				Log.particleAtTimed(rayPoint, ParticleEffect.FLAME)
			}
		}

		return cancel
	}

	fun onEnderPearlThrown(player: Player, sourceLoc: Location, targetLoc: Location): Location? {
		var newTarget: Location? = targetLoc

		//cancel pearl if source is protected (feet or eyes)
		val source = when(player.isInsideVehicle) {
			true -> Point(sourceLoc).add(0, 1, 0) //player technically has eyes in vehicle but practically speaking player has feet in vehicle
			else -> Point(sourceLoc)
		}
		val world = targetLoc.world
		val feet = source
		val eyes = source.add(0, 1, 0)
		if (feet.isProtected(world) || eyes.isProtected(world)) {
			onCancelPearl(player, "Pearling while inside a fortress wall is not allowed.")
			newTarget = null //cancel
		}
		if (newTarget == null) return null //cancel teleport

		//cancel pearl if target or above is protected
		val target = Point(targetLoc)
		val above = target.add(0, 1, 0)
		if (target.isProtected(world) || above.isProtected(world)) {
			onCancelPearl(player, "Pearling into a fortress wall is not allowed.")
			newTarget = null //cancel
		}
		if (newTarget == null) return null //cancel teleport

		//enforce min edge distance if target or above is claimed (cancel if can't enforce)
		if (target.isClaimed(world) || above.isClaimed(world)) {
			if (target.isAiry(world)) {
				//enforce 0.31 minimum distance from edge of block
				newTarget.enforceMinEdgeDist(0.31)
				newTarget.y = newTarget.blockY.toDouble() //y = y - y % 1
			} else {
				//cancel because can't safely floor y (player could be standing on slab)
				onCancelPearl(player, "Pearl glitch by fortress wall is not allowed.")
				newTarget = null //cancel
			}
		}

		return newTarget
	}
	private fun onCancelPearl(player: Player, msg: String) {
		player.sendMessage(ChatColor.AQUA.toString() + msg)
		player.inventory.addItem(ItemStack(Material.ENDER_PEARL))
	}
}