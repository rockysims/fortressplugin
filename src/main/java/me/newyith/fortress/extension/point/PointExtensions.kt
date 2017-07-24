package me.newyith.fortress.extension.point

import me.newyith.fortress.protection.ProtectionManager
import me.newyith.util.Point
import org.bukkit.World

fun Point.isProtected(world: World): Boolean {
	return ProtectionManager.forWorld(world).isProtected(this);
}