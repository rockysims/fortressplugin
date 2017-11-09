package me.newyith.fortress.rune.generator

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.main.FortressPlugin
import me.newyith.fortress.persist.SaveLoad
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.World

class GeneratorRuneId(
	@JsonProperty("worldName") val worldName: String,
	@JsonProperty("anchorPoint") val anchorPoint: Point
) {
	val savePath get() = SaveLoad.getSavePathOfGeneratorRuneById(this)

	val world: World by lazy {
		Bukkit.getWorld(worldName)
	}

	val generatorRune: GeneratorRune? by lazy {
		FortressPlugin.forWorld(world).loadedGeneratorRuneById[this]
	}
	//justification for using 'by lazy':
	//lazy would be good except that GeneratorRuneId is supposed to be very light weight
	//	shouldn't matter since we're just storing a reference to generateRune not creating one, right?
	//		and it doesn't need to get saved to json so no problem there
	//		and it's lazy so no need to set it when loading

	override fun toString(): String {
		return worldName + "@" + anchorPoint
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || other !is GeneratorRuneId) return false

		return this.worldName == other.worldName
			&& this.anchorPoint == other.anchorPoint
	}

	override fun hashCode(): Int {
		return toString().hashCode()
	}
}