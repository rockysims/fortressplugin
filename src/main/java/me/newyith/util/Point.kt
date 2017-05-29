package me.newyith.util

import org.bukkit.Location
import org.bukkit.block.Block

class Point {
	private data class Model(
		val x: Double,
		val y: Double,
		val z: Double)

	private var model: Model

	constructor(loc: Location) {
		val x = loc.x
		val y = loc.y
		val z = loc.z
		model = Model(x, y, z)
	}

	constructor(b: Block) : this(b.location)

	// --- //

	private fun x(): Double = model.x
	private fun y(): Double = model.y
	private fun z(): Double = model.z

	private fun xInt(): Int = model.x.toInt()
	private fun yInt(): Int = model.y.toInt()
	private fun zInt(): Int = model.z.toInt()

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
			equal = equal && Math.floor(x()) == Math.floor(p.x())
			equal = equal && Math.floor(y()) == Math.floor(p.y())
			equal = equal && Math.floor(z()) == Math.floor(p.z())

			return equal
		} else {
			return false
		}
	}

	override fun hashCode(): Int {
		var hash = Math.floor(x()).toInt()
		hash = 49999 * hash + Math.floor(y()).toInt()
		hash = 49999 * hash + Math.floor(z()).toInt()
		return hash
	}
}
