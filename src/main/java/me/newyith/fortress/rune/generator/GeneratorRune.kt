package me.newyith.fortress.rune.generator

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.World
import org.bukkit.entity.Player

class GeneratorRune (
	@JsonProperty("pattern") val pattern: GeneratorRunePattern
) {
	@Transient val world: World = pattern.world
	@Transient val anchor: Point = pattern.anchorPoint

	fun onCreated(player: Player) {
		Log.log("//TODO: handle GeneratorRune::onCreated() called. player: " + player.name)
	}
}