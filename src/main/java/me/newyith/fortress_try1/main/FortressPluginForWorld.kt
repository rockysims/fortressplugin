package me.newyith.fortress_try1.main

import me.newyith.fortress_try1.extension.point.isProtected
import me.newyith.fortress_try1.protection.ProtectionManager
import me.newyith.fortress_try1.rune.generator.GeneratorRune
import me.newyith.fortress_try1.rune.generator.GeneratorRunePatterns
import me.newyith.util.Log
import me.newyith.util.Point
import me.newyith.util.extension.material.isDoor
import me.newyith.util.extension.material.isTallDoor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.material.Door
import org.bukkit.material.Openable
import org.bukkit.material.TrapDoor
import java.util.*

class FortressPluginForWorld(val world: World) {
	private val generatorRunes = HashSet<GeneratorRune>()
	private val generatorRuneByPatternPoint = HashMap<Point, GeneratorRune>()
	private val generatorRuneByClaimedWallPoint = HashMap<Point, GeneratorRune>() //TODO: keep updated
	private val claimedPoints = HashSet<Point>() //TODO: keep updated
	private val saveLoadManager = FortressPlugin.getSaveLoadManager()

	fun load() {
		Log.log(world.name + " FortressPluginForWorld::enable() called") //TODO: delete this line

		//load generatorRunes
		generatorRunes.clear()
		generatorRunes.addAll(saveLoadManager.loadGeneratorRunes(world))

		for (generatorRune in generatorRunes) {
			//rebuild generatorRuneByPatternPoint
			for (patternPoint in generatorRune.pattern.points) {
				generatorRuneByPatternPoint.put(patternPoint, generatorRune)
			}

			//rebuild generatorRuneByClaimedWallPoint
			for (p in generatorRune.generatorCore.getClaimedWallPoints()) {
				generatorRuneByClaimedWallPoint.put(p, generatorRune)
			}

			//rebuild claimedPoints
			claimedPoints.addAll(generatorRune.generatorCore.getClaimedPoints())

			//TODO: all generatorRune data should be stored in generatorRune so rebuild data in ProtectionManager here
			//register protection granted by generatorRunes
			for (generatorRune in generatorRunes) {
				ProtectionManager.forCoreId(generatorRune.coreId).protect(generatorRune.generatorCore.getProtectedBatches())
			}
		}
	}

	fun save() {
		Log.log(world.name + " FortressPluginForWorld::disable() called") //TODO: delete this line

		//save generatorRunes
		generatorRunes.parallelStream().forEach {
			saveLoadManager.saveGeneratorRune(it)
		}
	}

	fun getGeneratorRuneByClaimedWallPoint(p: Point): GeneratorRune? {
		return generatorRuneByClaimedWallPoint[p]
	}

	fun isClaimed(p: Point): Boolean {
		return p in claimedPoints
	}

	fun onTick() {
		generatorRunes.forEach(GeneratorRune::onTick)
	}

	fun onSignChange(player: Player, signBlock: Block): Boolean {
		var cancel = false

		val runePattern = GeneratorRunePatterns.tryReadyPattern(signBlock)
		runePattern?.let { runePattern ->
			val runeAlreadyCreated = Point(signBlock) in generatorRuneByPatternPoint.keys
			if (!runeAlreadyCreated) {
				val generatorRune = GeneratorRune(runePattern)
				generatorRunes.add(generatorRune)

				//add new generatorRune to generatorRuneByPoint map
				runePattern.points.forEach {
					generatorRuneByPatternPoint.put(it, generatorRune)
				}

				generatorRune.onCreated(player)
				cancel = true //otherwise initial text on sign is replaced by what user wrote
			} else {
				player.sendMessage(ChatColor.AQUA.toString() + "Failed to create rune because rune already created here.")
			}
		}

		return cancel
	}

	fun onPlayerRightClickBlock(player: Player, block: Block, face: BlockFace) {
		generatorRuneByClaimedWallPoint[Point(block)]
			?.onPlayerRightClickWall(player, block, face)
	}

	fun onPlayerOpenCloseDoor(player: Player, doorBlock: Block): Boolean {
		var cancel = false

		val doorPoint = Point(doorBlock)
		if (doorPoint.isProtected(world)) {
			val rune = generatorRuneByClaimedWallPoint[doorPoint]
			when (rune) {
				null -> Log.error("FortressPluginForWorld::onPlayerOpenCloseDoor() failed to find rune from doorPoint " + doorPoint)
				else -> {
					val aboveDoorPoint = doorPoint.add(0, 1, 0)
					val aboveDoorType = aboveDoorPoint.getType(world)
					val topDoorPoint = when (aboveDoorType.isTallDoor()) {
						true -> aboveDoorPoint
						false -> doorPoint
					}
					val canOpen = rune.generatorCore.playerCanOpenCloseDoor(player, topDoorPoint)
					if (!canOpen) {
						cancel = true
					} else {
						//if iron door, open for player
						val doorType = topDoorPoint.getType(world)
						val isIronTallDoor = doorType == Material.IRON_DOOR_BLOCK
						val isIronTrapDoor = doorType == Material.IRON_TRAPDOOR
						if (isIronTallDoor || isIronTrapDoor) {
							val nowOpen: Boolean

							if (isIronTallDoor) {
								var state = doorBlock.state
								var door = state.data as Door
								if (door.isTopHalf) {
									val bottomDoorBlock = topDoorPoint.add(0, -1, 0).getBlock(world)
									state = bottomDoorBlock.state
									door = state.data as Door
								}
								door.isOpen = !door.isOpen
								state.update()
								nowOpen = door.isOpen
							} else {
								val state = doorBlock.state
								val door = state.data as TrapDoor
								door.isOpen = !door.isOpen
								state.update()
								nowOpen = door.isOpen
							}

							if (nowOpen) {
								player.playSound(topDoorPoint.toLocation(world), Sound.DOOR_OPEN, 1.0f, 1.0f)
							} else {
								player.playSound(topDoorPoint.toLocation(world), Sound.DOOR_CLOSE, 1.0f, 1.0f)
							}
						}
					}
				}
			}
		}

		return cancel
	}

	fun onPlayerCloseChest(player: Player, block: Block) {
		val p = Point(block)
		generatorRuneByPatternPoint[p]?.onPlayerCloseChest(player, p)
	}

	fun onBlockRedstoneEvent(signal: Int, block: Block): Int? {
		val p = Point(block)

		//if the redstone that changed is part of the rune, update rune state
		generatorRuneByPatternPoint[p]?.setPowered(signal > 0)

		//if door is protected, ignore redstone event
		if (p.isProtected(world) && block.type.isDoor()) {
			val openableDoor = block.state.data as Openable
			return if (openableDoor.isOpen) 1 else 0
		} else {
			return null
		}
	}

	fun onEnvironmentBreaksRedstoneWireEvent(brokenBlock: Block) {
		onRuneMightHaveBeenBrokenBy(brokenBlock)
	}

	fun onBlockBreakEvent(player: Player, brokenBlock: Block): Boolean {
		var cancel = false

		val inCreative = player.gameMode == GameMode.CREATIVE
		if (!inCreative) {
			val brokenPoint = Point(brokenBlock)
			if (brokenPoint.isProtected(world)) { //was isGenerated()
				cancel = true
				//was getRuneByClaimedWallPoint()...
				generatorRuneByClaimedWallPoint[brokenPoint]?.let { generatorRune ->
					generatorRune.generatorCore.shield(brokenPoint)
				}
			}

			//commented out because we're not gonna bother handling piston special case yet (handling it here is not elegant)
//			if (!isProtected) {
//				when (brokenPoint.getType(world)) {
//					Material.PISTON_EXTENSION, Material.PISTON_MOVING_PIECE -> {
//						val matData = brokenBlock.getState().getData()
//						if (matData is PistonExtensionMaterial) {
//							val pem = matData as PistonExtensionMaterial
//							val face = pem.getFacing().getOppositeFace()
//							val pistonBasePoint = Point(brokenBlock.getRelative(face, 1))
//							if (model.protectedPoints.contains(pistonBasePoint)) {
//								cancel = true
//								if (rune != null) rune!!.getGeneratorCore().shield(brokenPoint)
//							}
//						} else {
//							Debug.error("matData not instanceof PistonExtensionMaterial")
//						}
//					}
//				}
//			}
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(brokenBlock)
		}

		return cancel
	}

	fun onBlockPlaceEvent(player: Player, placedBlock: Block, replacedMaterial: Material): Boolean {
		var cancel = false

		when (replacedMaterial) {
			Material.STATIONARY_WATER,
			Material.STATIONARY_LAVA
			-> {
				val placedPoint = Point(placedBlock)
				val isProtected = ProtectionManager.forWorld(world).isProtected(placedPoint)
				val inCreative = player.gameMode == GameMode.CREATIVE
				if (isProtected && !inCreative) {
					cancel = true
				}
			}
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(placedBlock)
		}

		return cancel
	}

	private fun onRuneMightHaveBeenBrokenBy(block: Block) {
		generatorRuneByPatternPoint[Point(block)]?.let { breakRune(it) }
	}

	fun breakRune(generatorRune: GeneratorRune) {
		generatorRune.onBroken()
		//breaking should naturally update: generatedPoints and generatorRuneByProtectedPoint

		for (p in generatorRune.pattern.points) {
			generatorRuneByPatternPoint.remove(p)
		}

		generatorRunes.remove(generatorRune)
	}










	fun onPistonEvent(isSticky: Boolean, piston: Point, target: Point?, movedBlocks: Set<Block>): Boolean {
		var cancel = false

		if (!cancel) {
			//fill pointsAffected
			val pointsAffected = HashSet<Point>()
			pointsAffected.add(piston)
			target?.let { pointsAffected.add(it) }
			//add movedBlocks to pointsAffected
			movedBlocks.mapTo(pointsAffected) { Point(it) }

			//fill runesAffected
			val runesAffected = HashSet<GeneratorRune>()
			for (p in pointsAffected) {
				generatorRuneByPatternPoint[p]?.let { rune ->
					runesAffected.add(rune)
				}
			}

			//break affected runes
			runesAffected.forEach { breakRune(it) }
		}

		return cancel
	}

	fun onExplode(explodeBlocks: Set<Block>) {
		explodeBlocks.forEach { onRuneMightHaveBeenBrokenBy(it) }
	}

	fun onBurn(b: Block): Boolean {
		onRuneMightHaveBeenBrokenBy(b)
		return false //don't cancel
	}

	fun onEndermanPickupBlock(b: Block): Boolean {
		onRuneMightHaveBeenBrokenBy(b)
		return false //don't cancel
	}

	fun onZombieBreakBlock(b: Block): Boolean {
		onRuneMightHaveBeenBrokenBy(b)
		return false //don't cancel
	}
}




















