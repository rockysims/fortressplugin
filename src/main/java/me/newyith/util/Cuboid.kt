package me.newyith.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Bukkit
import org.bukkit.World

class Cuboid (
	@JsonProperty("worldName") private val worldName: String,
	@JsonProperty("minimumPoint") private val minimumPoint: Point,
	@JsonProperty("maximumPoint") private val maximumPoint: Point
) {
	@Transient val world: World = Bukkit.getWorld(worldName)

	init {
		Log.log("//TODO: write Cuboid")
	}
}
