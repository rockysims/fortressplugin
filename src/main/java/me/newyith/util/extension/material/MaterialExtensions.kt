package me.newyith.util.extension.material

import org.bukkit.Material

fun Material.isDoor(): Boolean {
	return this.isTrapDoor() || this.isTallDoor()
}

fun Material.isTallDoor(): Boolean {
	return when (this) {
		Material.IRON_DOOR_BLOCK,
		Material.WOODEN_DOOR,
		Material.ACACIA_DOOR,
		Material.BIRCH_DOOR,
		Material.DARK_OAK_DOOR,
		Material.JUNGLE_DOOR,
		Material.SPRUCE_DOOR -> true
		else -> false
	}
}

fun Material.isTrapDoor(): Boolean {
	return when (this) {
		Material.TRAP_DOOR,
		Material.IRON_TRAPDOOR -> true
		else -> false
	}
}