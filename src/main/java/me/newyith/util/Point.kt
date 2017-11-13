package me.newyith.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.Vector

open class Point (
	@JsonProperty("x") val x: Double,
	@JsonProperty("y") val y: Double,
	@JsonProperty("z") val z: Double
) {
	constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
	constructor(loc: Location) : this(loc.x, loc.y, loc.z)
	constructor(v: Vector) : this(v.x, v.y, v.z)
	constructor(b: Block) : this(b.location)

	// --- //

	val xInt
		get() = x.toInt()
	val yInt
		get() = y.toInt()
	val zInt
		get() = z.toInt()

	fun getChunk(world: World): Chunk {
		return toLocation(world).chunk
	}

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

	fun distance(p: Point): Double {
		val v1 = this.toVector()
		val v2 = p.toVector()
		return v1.distance(v2)
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
		return world.getBlockAt(xInt, yInt, zInt)
	}

	fun getType(world: World): Material {
		return getBlock(world).type
	}

	fun getAdjacent6(): Set<Point> {
		val points = HashSet<Point>()

		points.add(this.add(1, 0, 0))
		points.add(this.add(-1, 0, 0))
		points.add(this.add(0, 1, 0))
		points.add(this.add(0, -1, 0))
		points.add(this.add(0, 0, 1))
		points.add(this.add(0, 0, -1))

		return points
	}

	fun getAdjacent26(): Set<Point> {
		val points = HashSet<Point>()
		val range = arrayOf(-1, 0, 1)

		for (x in range) {
			for (y in range) {
				for (z in range) {
					val isCenter = x == 0 && y == 0 && z == 0
					if (!isCenter) points.add(this.add(x, y, z))
				}
			}
		}

		return points
	}

	// - Overrides - //

	override fun toString(): String {
		val s = StringBuilder()
		s.append(xInt)
		s.append(", ")
		s.append(yInt)
		s.append(", ")
		s.append(zInt)
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
