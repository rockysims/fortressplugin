package me.newyith.fortress.core

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player

class GeneratorCore (
	@JsonProperty("worldName") val worldName: String,
	@JsonProperty("anchorPoint") val anchorPoint: Point,
	@JsonProperty("coreMats") val coreMats: CoreMaterials
) {
	@Transient val world: World = Bukkit.getWorld(worldName)

	init {
		Log.log("//TODO: write GeneratorCore")
	}

	fun onGeneratedChanged() {
		//TODO: write
	}

	val layerOutsideFortress: Set<Point>
		get() {
			//TODO: write
			return HashSet()
		}

	fun tick() {
		//TODO: write
	}

	fun onCreated(player: Player): Boolean {
		//TODO: write
		return true
	}

	fun onBroken() {
		//TODO: write
	}

	fun onPlayerRightClickWall(player: Player, block: Block, face: BlockFace) {
		//TODO: write
	}

	fun getClaimedWallPoints(): Set<Point> {
		//TODO: write
		//	consider changing to a val get() =
		return HashSet()
	}

	fun getInvalidWallMaterials(): Set<Material> {
		//TODO: write
		return HashSet()
	}

	fun setActive(active: Boolean) {
		//TODO: write
	}

	fun playerCanOpenCloseDoor(player: Player, topDoorPoint: Point): Boolean {
		//TODO: write
		return true
	}

	fun shield(shieldPoint: Point) {
		//TODO: write
	}

}