package me.newyith.fortress.protection

import me.newyith.util.Point
import org.bukkit.World
import org.bukkit.block.Block

/**
 * Handles enforcement of protected points.
 * Transient. Does not load/save data onEnable/Disable.
 */
class ProtectionManagerForWorld(world: World) {
	fun  isProtected(p: Point): Boolean {
		//TODO: write
		return false
	}

	fun onPistonEvent(movedBlocks: Set<Block>): Boolean {
		return movedBlocks.any { isProtected(Point(it)) }
	}
}