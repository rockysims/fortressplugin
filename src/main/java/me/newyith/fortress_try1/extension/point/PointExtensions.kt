package me.newyith.fortress_try1.extension.point

import me.newyith.fortress_try1.main.FortressPlugin
import me.newyith.fortress_try1.protection.ProtectionManager
import me.newyith.util.Point
import org.bukkit.Material
import org.bukkit.World

fun Point.isProtected(world: World): Boolean {
	return ProtectionManager.forWorld(world).isProtected(this)
}

fun Point.isClaimed(world: World): Boolean {
	return FortressPlugin.forWorld(world).isClaimed(this)
}

fun Point.isAiry(world: World): Boolean {
	return when(this.getType(world)) {
		Material.AIR,
		Material.LONG_GRASS,
		Material.RED_ROSE,
		Material.YELLOW_FLOWER,
		Material.DOUBLE_PLANT,
		Material.DEAD_BUSH,
		Material.SUGAR_CANE_BLOCK,
		Material.SAPLING,
		Material.CROPS,
		Material.POTATO -> true
		else -> false
	}
}

fun Point.getAdjacent6(): Set<Point> {
	val points = HashSet<Point>()

	points.add(this.add(1, 0, 0))
	points.add(this.add(-1, 0, 0))
	points.add(this.add(0, 1, 0))
	points.add(this.add(0, -1, 0))
	points.add(this.add(0, 0, 1))
	points.add(this.add(0, 0, -1))

	return points
}

fun Point.getAdjacent26(): Set<Point> {
	val points = HashSet<Point>()
	val range = arrayOf(-1, 0, 1)

	for (x in range) {
		for (y in range) {
			for (z in range) {
				val isCenter = x == 0 && y == 0 && z == 0
				if (!isCenter) points.add(this.add(x, y, z))
			}
		}
	}

	return points
}
