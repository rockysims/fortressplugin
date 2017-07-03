package me.newyith.fortress.rune.generator

import me.newyith.fortress.core.CoreMaterials
import me.newyith.fortress.core.GeneratorCore
import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress.event.TickTimer
import me.newyith.fortress.main.FortressPlugin
import me.newyith.util.Cuboid
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.util.*
import java.util.stream.Collectors

class GeneratorRune(
	@JsonProperty("pattern") val pattern: GeneratorRunePattern,
	@JsonProperty("core") private val core: GeneratorCore,
	@JsonProperty("state") private var state: GeneratorState,
	@JsonProperty("fuelTicksRemaining") private var fuelTicksRemaining: Int,
	@JsonProperty("powered") private var powered: Boolean
) {
	val world: World
		get() = pattern.world
	val anchor: Point
		get() = pattern.anchorPoint

	@Transient private val powerToggleTimeStamps: MutableList<Long> = ArrayList()

	constructor(
		pattern: GeneratorRunePattern
	) : this(
		pattern,
		GeneratorCore(
			pattern.world.name,
			pattern.anchorPoint,
			CoreMaterials(pattern.world.name, pattern.chestPoint)
		),
		GeneratorState.NULL,
		fuelTicksRemaining = 0,
		powered = false
	)

	fun secondStageLoad() {
		core.onGeneratedChanged() //update which particles should be displayed (requires layerOutside already be filled)
		updatePoweredFromWorld() //powered state may have changed if server crashed so recheck
	}

	//-----------------------------------------------------------------------

	// - Getters -

	private val isRunning:Boolean
		get() = state == GeneratorState.RUNNING

	private val isPaused:Boolean
		get() = state == GeneratorState.PAUSED

	private val isPowered:Boolean
		get() = powered

	val generatorCore:GeneratorCore
		get() = core

	val layerOutsideFortress:Set<Point>
		get() = core.layerOutsideFortress

	val fortressCuboid:Cuboid
		get() {
			val min = anchor.toVector()
			val max = anchor.toVector()

			val points = HashSet<Point>()
			points.addAll(pattern.points)
			points.addAll(core.getClaimedWallPoints())
			for (p in points) {
				min.setX(Math.min(min.getX(), p.x))
				min.setY(Math.min(min.getY(), p.y))
				min.setZ(Math.min(min.getZ(), p.z))
				max.setX(Math.max(max.getX(), p.x))
				max.setY(Math.max(max.getY(), p.y))
				max.setZ(Math.max(max.getZ(), p.z))
			}

			//min-- and max++ (so as to include claimed points)
			val minPoint = Point(min).add(-1, -1, -1)
			val maxPoint = Point(max).add(1, 1, 1)

			return Cuboid(world.name, minPoint, maxPoint)
		}

	// - Events -

	fun onTick() {
		tickFuel()
		core.tick()
	}

	fun onCreated(player: Player) {
		moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint)
		moveBlockTo(Material.DIAMOND_BLOCK, pattern.anchorPoint)

		updatePoweredFromWorld() //initialize this.powered
		updateState()

		val placed = core.onCreated(player)
		if (!placed)
		{
			val world = pattern.world
			FortressPlugin.forWorld(world).breakRune(this)
		}
	}

	fun onBroken() {
		moveBlockTo(Material.DIAMOND_BLOCK, pattern.runningPoint)
		moveBlockTo(Material.GOLD_BLOCK, pattern.anchorPoint)
		setSignText("Broken", "", "")

		core.onBroken()
	}

	fun onPlayerRightClickWall(player: Player, block: Block, face: BlockFace) {
		core.onPlayerRightClickWall(player, block, face)
	}

	private fun updatePoweredFromWorld() {
		val powered = pattern.wirePoint.getBlock(world).blockPower > 0
		setPowered(powered)
	}

	fun setPowered(powered: Boolean) {
		if (this.powered != powered) {
			if (countRecentPowerToggles() > 10) {
				FortressPlugin.forWorld(world).breakRune(this)
			} else {
				powerToggleTimeStamps.add(System.currentTimeMillis()) //used by countRecentPowerToggles()
				this.powered = powered
				updateState()
			}
		}
	}

	//TODO: call this from GeneratorCore
	fun onSearchingChanged(searching: Boolean) {
		//TODO: comment out again (see next line)
		//* //commented out because flashing "Searching" for a fraction of a second looks bad
		if (searching) {
			setSignText("Searching", null, null)
		} else {
			//set line1 back to state
			when (state) {
				GeneratorState.RUNNING -> setSignText("Running", null, null)
				GeneratorState.PAUSED -> setSignText("Paused", null, null)
				GeneratorState.NEEDS_FUEL -> setSignText("Needs Fuel", null, null)
				GeneratorState.NULL -> {
					Log.error("GeneratorRune::onSearchingChanged() found invalid state: " + state)
					setSignText("NULL?", null, null)
				}
			}
		}
		//*/
	}

	fun onPlayerCloseChest(player: Player, chestPoint: Point) {
		if (chestPoint == pattern.chestPoint) {
			val invalidMaterials = core.getInvalidWallMaterials().stream()
				.filter({ material -> material.isBlock })
				.collect(Collectors.toSet())
			if (invalidMaterials.isNotEmpty()) {
				var msg = "Fortress generator can't protect:\n"
				msg += invalidMaterials.map { it.toString() }.joinToString("\n") //TODO: test this displays the right thing
				player.sendMessage(ChatColor.AQUA.toString() + msg)
			}
		}
	}

	// - Handlers -

	private fun tickFuel() {
		if (fuelTicksRemaining > 0 && isRunning) {
			fuelTicksRemaining--
		}

		if (fuelTicksRemaining <= 0) {
			tryReplenishFuel()
			updateState()
		}

		//always update sign in case amount of fuel in chest has changed
		updateFuelRemainingDisplay((fuelTicksRemaining * TickTimer.msPerTick).toLong())
	}

	private fun tryReplenishFuel() {
		chest?.let { chest ->
			if (Material.GLOWSTONE_DUST in chest.inventory) {
				chest.inventory.removeItem(ItemStack(Material.GLOWSTONE_DUST, 1))
				chest.update(true)

				fuelTicksRemaining = FortressPlugin.getConfig().glowstoneDustBurnTimeMs / TickTimer.msPerTick
				updateFuelRemainingDisplay((fuelTicksRemaining * TickTimer.msPerTick).toLong())
			}
		}
	}

	private fun updateState() {
		if (fuelTicksRemaining <= 0) {
			tryReplenishFuel()
		}

		if (fuelTicksRemaining > 0) {
			//TODO: make isPowered mean running instead of paused and vic versa
			if (isPowered) setState(GeneratorState.PAUSED)
			else setState(GeneratorState.RUNNING)
		} else {
			setState(GeneratorState.NEEDS_FUEL)
		}
	}

	private fun setState(state: GeneratorState) {
		if (this.state != state) {
			when (state) {
				GeneratorState.RUNNING -> {
					setSignText("Running", "", null)
					moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint)
				}
				GeneratorState.PAUSED -> {
					setSignText("Paused", "", null)
					moveBlockTo(Material.GOLD_BLOCK, pattern.pausePoint)
				}
				GeneratorState.NEEDS_FUEL -> {
					setSignText("Needs Fuel", "(glowstone dust)", "")
					moveBlockTo(Material.GOLD_BLOCK, pattern.fuelPoint)
				}
				else -> {
					Log.error("GeneratorRune::setState() couldn't find a case matching GeneratorState: " + state)
				}
			}
			//world.playSound(anchor, Sound.SHEEP_SHEAR, 5, 1); //5 (volume), 1 (pitch) is hopefully normal
			//world.playSound(anchor, Sound.IRONGOLEM_THROW, 5, 1);

			this.state = state
			core.setActive(state == GeneratorState.RUNNING)
		}
	}

	// - Utils -

	fun countGlowstoneDustInChest(): Int {
		var count = 0
		if (chest != null)
		{
			val inv = chest!!.getInventory()
			val items = inv.getContents()
			for (item in items)
			{
				if (item != null && item!!.getType() == Material.GLOWSTONE_DUST)
				{
					count += item!!.getAmount()
				}
			}
		}
		return count
	}

	private val chest: Chest?
		get() {
			val blockState = pattern.chestPoint.getBlock(world).state
			return when(blockState) {
				is Chest -> blockState
				else -> {
					Log.error("GeneratorRune failed to find chest at chestPoint.")
					null
				}
			}
		}

	private fun updateFuelRemainingDisplay(ms: Long) {
		var ms = ms
		val glowstoneDustInChest = countGlowstoneDustInChest()
		ms += (FortressPlugin.getConfig().glowstoneDustBurnTimeMs * glowstoneDustInChest).toLong()

		var s = ms / 1000
		var m = s / 60
		var h = m / 60
		val d = h / 24
		h = h % 24
		m = m % 60
		s = s % 60
		val str = StringBuilder()
		if (d > 0) str.append(d.toString() + "d ")
		if (h > 0) str.append(h.toString() + "h ")
		if (m > 0) str.append(m.toString() + "m ")
		if (s > 0) str.append(s.toString() + "s")
		setSignText(null, null, str.toString())
	}

	private fun setSignText(line1: String?, line2: String?, line3: String?): Boolean {
		val signPoint = pattern.signPoint
		val signBlock = signPoint.getBlock(world)
		val blockState = signBlock.state
		if (blockState is Sign) {
			val sign = blockState
			sign.setLine(0, "Generator:")
			if (line1 != null) sign.setLine(1, line1)
			if (line2 != null) sign.setLine(2, line2)
			if (line3 != null) sign.setLine(3, line3)
			sign.update()
			return true
		} else {
			//this can happen when a sign is exploded (generator broken check has to delay while explosion to finishes)
			Log.warn("setSignText() failed to find sign at " + signPoint)
		}
		return false
	}

	private fun moveBlockTo(material: Material, targetPoint: Point) {
		val points = ArrayList<Point>()
		points.add(pattern.anchorPoint)
		points.add(pattern.runningPoint)
		points.add(pattern.pausePoint)
		points.add(pattern.fuelPoint)
		val materialPoint: Point? = points.firstOrNull { it.isType(material, world) }
		materialPoint?.let { materialPoint ->
			swapBlocks(materialPoint, targetPoint)
		}
	}

	private fun swapBlocks(a: Point, b: Point) {
		val aMat = a.getBlock(world).type
		val bMat = b.getBlock(world).type
		a.getBlock(world).type = bMat
		b.getBlock(world).type = aMat
	}

	private fun countRecentPowerToggles(): Int {
		//set count recent power toggles and remove expired stamps
		val now = System.currentTimeMillis()
		val stampLifetimeMs = 5 * 1000
		var count = 0
		val it = powerToggleTimeStamps.iterator()
		while (it.hasNext()) {
			val stamp = it.next()
			if (now - stamp < stampLifetimeMs) {
				count++
			} else {
				it.remove()
			}
		}

		return count
	}

	override fun equals(obj: Any?): Boolean {
		return obj is GeneratorRune && anchor === obj.pattern.anchorPoint
	}

	override fun hashCode(): Int {
		return anchor.hashCode()
	}
}
