package me.newyith.fortress.extension.block

import me.newyith.fortress.extension.point.isProtected
import me.newyith.util.Point
import org.bukkit.block.Block

fun Block.isProtected(): Boolean {
	return Point(this).isProtected(world)
}