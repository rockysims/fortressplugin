package me.newyith.fortress.core

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.World

class CoreMaterials (
	@JsonProperty("worldName") val worldName: String,
	@JsonProperty("chestPoint") val chestPoint: Point
) {
	@Transient val world: World = Bukkit.getWorld(worldName)

	init {
		Log.log("//TODO: write CoreMaterials")
	}
}