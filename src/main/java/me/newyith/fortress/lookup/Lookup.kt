package me.newyith.fortress.lookup

import org.bukkit.Bukkit
import org.bukkit.World

class Lookup(val worldName: String) {
//	private val generatorRuneIdByClaimedWallOrPatternPoint = HashMap<Point, GeneratorRuneId>() //TODO: keep updated

	val world: World by lazy {
		Bukkit.getWorld(worldName)
	}

	//
}