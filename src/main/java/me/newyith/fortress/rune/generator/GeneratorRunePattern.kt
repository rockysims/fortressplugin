package me.newyith.fortress.rune.generator

import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import java.util.HashSet

class GeneratorRunePattern (
		val worldName: String,
		val signPoint: Point,
		val wirePoint: Point,
		val anchorPoint: Point,
		val chestPoint: Point,
		val pausePoint: Point,
		val runningPoint: Point,
		val fuelPoint: Point
	) {
	@Transient val world: World = Bukkit.getWorld(worldName)
	@Transient val points = HashSet<Point>()

	init {
		//rebuild transient fields
		points.add(signPoint)
		points.add(wirePoint)
		points.add(anchorPoint)
		points.add(chestPoint)
		points.add(pausePoint)
		points.add(runningPoint)
		points.add(fuelPoint)
	}

	operator fun contains(p: Point): Boolean {
		return points.contains(p)
	}

	val isValid: Boolean
		get() {
			var valid = true
			valid = valid && signPoint.isType(Material.WALL_SIGN, world)
			valid = valid && anchorPoint.isType(Material.DIAMOND_BLOCK, world)
			valid = valid && wirePoint.isType(Material.REDSTONE_WIRE, world)
			valid = valid && chestPoint.isType(Material.CHEST, world)

			var goldCount = 0
			var ironCount = 0
			val points = HashSet<Point>()
			points.add(pausePoint)
			points.add(runningPoint)
			points.add(fuelPoint)
			points.forEach {
				when (it.getType(world)) {
					Material.IRON_BLOCK -> ironCount++
					Material.GOLD_BLOCK -> goldCount++
					else -> {}
				}
			}
			valid = valid && goldCount == 1
			valid = valid && ironCount == 2

			return valid
		}
}
