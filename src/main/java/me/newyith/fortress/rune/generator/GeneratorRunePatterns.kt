package me.newyith.fortress.rune.generator

import me.newyith.util.Point
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.material.Sign

object GeneratorRunePatterns {
	//TODO: double check it finds ready pattern in all 4 orientations
	fun tryReadyPattern(signBlock: Block): GeneratorRunePattern? {
		var pattern: GeneratorRunePattern? = null

		//if (found sign)
		if (signBlock.type == Material.WALL_SIGN) {
			val world = signBlock.world
			val s = Point(signBlock)
			val a = getPointSignAttachedTo(signBlock)

			//if (found anchor)
			if (a.isType(Material.GOLD_BLOCK, world)) {
				//set towardFront, towardBack, towardLeft, towardRight
				val towardFront = s.difference(a)
				val towardLeft = when(towardFront.x == 0.0) {
					true -> Point(-1 * towardFront.z, 0.0, -1 * towardFront.x)
					false -> Point(towardFront.z, 0.0, towardFront.x)
				}
				val towardBack = Point(-1 * towardFront.x, 0.0, -1 * towardFront.z)
				val towardRight = Point(-1 * towardLeft.x, 0.0, -1 * towardLeft.z)

				//find remaining points
				var w = a.add(towardLeft)
				var c = a.add(towardRight)
				var p = a.add(0, -1, 0).add(towardLeft)
				val r = a.add(0, -1, 0)
				var f = a.add(0, -1, 0).add(towardRight)
				if (c.isType(Material.REDSTONE_WIRE, world) && w.isType(Material.CHEST, world)) {
					var t: Point
					//reverse wire / chest
					t = w
					w = c
					c = t
					//reverse pause / fuel
					t = p
					p = f
					f = t
				}

				//check other blocks match ready pattern
				var valid = true
				valid = valid && w.isType(Material.REDSTONE_WIRE, world)
				valid = valid && c.isType(Material.CHEST, world)
				valid = valid && p.isType(Material.IRON_BLOCK, world)
				valid = valid && r.isType(Material.DIAMOND_BLOCK, world)
				valid = valid && f.isType(Material.IRON_BLOCK, world)

				if (valid) {
					pattern = GeneratorRunePattern(world.name, s, w, a, c, p, r, f)
				}
			}
		}

		return pattern
	}

	private fun getPointSignAttachedTo(signBlock: Block): Point {
		val s = Point(signBlock)
		val sign = signBlock.state.data as Sign
		val af = sign.attachedFace
		return s.add(af.modX, af.modY, af.modZ)
	}
}