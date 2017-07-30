package me.newyith.fortress.extension.point

import me.newyith.fortress.main.FortressPlugin
import me.newyith.fortress.protection.ProtectionManager
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