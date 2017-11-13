package me.newyith.fortress_try1.extension.location

import org.bukkit.Location

fun Location.enforceMinEdgeDist(minDist: Double) {
	val x = this.x
	val y = this.y
	val z = this.z
	var xDecimal = x % 1
	var yDecimal = y % 1
	var zDecimal = z % 1
	val xWhole = x - xDecimal
	val yWhole = y - yDecimal
	val zWhole = z - zDecimal

	//enforce minDist minimum distance from edge
	val lowLimit = minDist
	val highLimit = 1 - minDist
	xDecimal = Math.max(lowLimit, Math.abs(xDecimal))
	xDecimal = Math.min(highLimit, Math.abs(xDecimal))
	yDecimal = Math.max(lowLimit, Math.abs(yDecimal))
	yDecimal = Math.min(highLimit, Math.abs(yDecimal))
	zDecimal = Math.max(lowLimit, Math.abs(zDecimal))
	zDecimal = Math.min(highLimit, Math.abs(zDecimal))
	if (x < 0) xDecimal *= -1
	if (y < 0) yDecimal *= -1
	if (z < 0) zDecimal *= -1

	//update location with new coordinates
	this.x = xWhole + xDecimal
	this.y = yWhole + yDecimal
	this.z = zWhole + zDecimal
}