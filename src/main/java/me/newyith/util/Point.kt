package me.newyith.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.Vector

class Point (
	@JsonProperty("x") val x: Double,
	@JsonProperty("y") val y: Double,
	@JsonProperty("z") val z: Double
) {
	constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
	constructor(loc: Location) : this(loc.x, loc.y, loc.z)
	constructor(v: Vector) : this(v.x, v.y, v.z)
	constructor(b: Block) : this(b.location)

	// --- //

	private fun xInt(): Int = x.toInt()
	private fun yInt(): Int = y.toInt()
	private fun zInt(): Int = z.toInt()

	fun toVector(): Vector {
		return Vector(x, y, z)
	}

	fun toLocation(world: World): Location {
		return Location(world, x, y, z)
	}

	fun difference(p: Point): Point {
		return Point(
			x - p.x,
			y - p.y,
			z - p.z
		)
	}

	fun add(p: Point): Point {
		return Point(
			x + p.x,
			y + p.y,
			z + p.z
		)
	}

	fun add(xAdd: Int, yAdd: Int, zAdd: Int): Point {
		return Point(
			x + xAdd,
			y + yAdd,
			z + zAdd
		)
	}

	fun add(xAdd: Double, yAdd: Double, zAdd: Double): Point {
		return Point(
			x + xAdd,
			y + yAdd,
			z + zAdd
		)
	}

	fun isType(mat: Material, world: World): Boolean {
		return getBlock(world).type == mat
	}

	fun getBlock(world: World): Block {
		return world.getBlockAt(xInt(), yInt(), zInt())
	}

	fun getType(world: World): Material {
		return getBlock(world).type
	}

	// - Overrides - //

	override fun toString(): String {
		val s = StringBuilder()
		s.append(xInt())
		s.append(", ")
		s.append(yInt())
		s.append(", ")
		s.append(zInt())
		return s.toString()
	}

	override fun equals(other: Any?): Boolean {
		if (other === this) return true

		if (other is Point) {
			val p = other

			var equal = true
			equal = equal && Math.floor(x) == Math.floor(p.x)
			equal = equal && Math.floor(y) == Math.floor(p.y)
			equal = equal && Math.floor(z) == Math.floor(p.z)

			return equal
		} else {
			return false
		}
	}

	override fun hashCode(): Int {
		var hash = Math.floor(x).toInt()
		hash = 49999 * hash + Math.floor(y).toInt()
		hash = 49999 * hash + Math.floor(z).toInt()
		return hash
	}
}
