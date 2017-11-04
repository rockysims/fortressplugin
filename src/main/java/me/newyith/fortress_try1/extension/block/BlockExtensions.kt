package me.newyith.fortress_try1.extension.block

import me.newyith.fortress_try1.extension.point.isProtected
import me.newyith.util.Point
import org.bukkit.block.Block

fun Block.isProtected(): Boolean {
	return Point(this).isProtected(world)
}