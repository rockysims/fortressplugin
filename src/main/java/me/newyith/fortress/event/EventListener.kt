package me.newyith.fortress.event

import me.newyith.fortress.main.FortressPlugin
import me.newyith.fortress.protection.ProtectionManager
import me.newyith.util.Log
import me.newyith.util.Point
import me.newyith.util.extension.material.isDoor
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Enderman
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.HashSet

class EventListener(plugin: JavaPlugin) : Listener {
	init {
		plugin.server.pluginManager.registerEvents(this, plugin)
	}

	//ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" at top of method
	@EventHandler(ignoreCancelled = true)
	fun onSignChange(event: SignChangeEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onSignChange(event.player, block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockBreakEvent(event: BlockBreakEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onBlockBreakEvent(event.player, block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onEnvironmentBreaksRedstoneWireEvent(event: BlockFromToEvent) {
		if (event.toBlock.type == Material.REDSTONE_WIRE) {
			val world = event.toBlock.world
			FortressPlugin.forWorld(world).onEnvironmentBreaksRedstoneWireEvent(event.toBlock)
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockRedstoneEvent(event: BlockRedstoneEvent) {
		val newCurrent = FortressPlugin.forWorld(event.block.world).onBlockRedstoneEvent(event.newCurrent, event.block)
		newCurrent?.let {
			event.newCurrent = newCurrent
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockPlaceEvent(event: BlockPlaceEvent) {
		val placedBlock = event.blockPlaced
		val replacedMaterial = event.blockReplacedState.type
		val cancel = FortressPlugin.forWorld(placedBlock.world).onBlockPlaceEvent(event.player, placedBlock, replacedMaterial)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockPistonExtendEvent(event: BlockPistonExtendEvent) {
		val block = event.block
		val world = block.world
		val piston = Point(block)
		val movedBlocks = HashSet(event.blocks)
		val target = piston.add(
			event.direction.modX,
			event.direction.modY,
			event.direction.modZ
		)

		val cancel = ProtectionManager.forWorld(world).onPistonEvent(movedBlocks)
			|| FortressPlugin.forWorld(world).onPistonEvent(event.isSticky, piston, target, movedBlocks)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockPistonRetractEvent(event: BlockPistonRetractEvent) {
		val block = event.block
		val piston = Point(block)
		val movedBlocks = HashSet(event.blocks)

		val cancel = FortressPlugin.forWorld(block.world).onPistonEvent(event.isSticky, piston, null, movedBlocks)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onExplode(event: EntityExplodeEvent) {
		val explodeBlocks = HashSet(event.blockList())
		val loc = event.location

		val cancel = FortressPlugin.forWorld(loc.world).onExplode(explodeBlocks, loc)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockIgnite(event: BlockIgniteEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onIgnite(block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onBlockBurn(event: BlockBurnEvent) {
		val block = event.block
		val cancel = FortressPlugin.forWorld(block.world).onBurn(block)
		if (cancel) event.isCancelled = true
	}

	@EventHandler(ignoreCancelled = true)
	fun onPlayerInteractEvent(event: PlayerInteractEvent) {
		val action = event.action
		if (action == Action.RIGHT_CLICK_BLOCK) {
			val clickedBlock = event.clickedBlock
			val world = clickedBlock.world
			val player = event.player

			FortressPlugin.forWorld(world).onPlayerRightClickBlock(player, clickedBlock, event.blockFace)

			if (clickedBlock.type.isDoor()) {
				val cancel = FortressPlugin.forWorld(world).onPlayerOpenCloseDoor(player, clickedBlock)
				if (cancel) event.isCancelled = true
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
		when (event.entity) {
			is Enderman -> {
				if (event.to == Material.AIR) {
					//picking up block
					val block = event.block
					val cancel = FortressPlugin.forWorld(block.world).onEndermanPickupBlock(block)
					if (cancel) event.isCancelled = true
				}
			}
			is Zombie -> {
				if (event.to == Material.AIR) {
					//destroying block
					val block = event.block
					val cancel = FortressPlugin.forWorld(block.world).onZombieBreakBlock(block)
					if (cancel) event.isCancelled = true
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onInventoryCloseEvent(event: InventoryCloseEvent) {
		val humanEntity = event.player
		if (humanEntity is Player) {
			val holder = event.inventory.holder
			if (holder is Chest) { //Trap Chest is also instanceof Chest
				val block = holder.block
				val world = block.world
				FortressPlugin.forWorld(world).onPlayerCloseChest(humanEntity, block)
			}
		}
	}

	internal var playersExitingVehicle: MutableSet<Player> = HashSet()
	@EventHandler(ignoreCancelled = true)
	fun onExitVehicle(event: VehicleExitEvent) {
		if (event.exited !is Player) return //if player

		//holding shift to exit vehicle calls this event many times very fast
		//so wait for handler method to finish before allowing it to be called again (per player)
		val player = event.exited as Player
		if (player !in playersExitingVehicle) {
			playersExitingVehicle.add(player)
			FortressPlugin.forWorld(player.world).onPlayerExitVehicle(player)
			playersExitingVehicle.remove(player)
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onEntityDamageFromExplosion(event: EntityDamageEvent) {
		if (event !is EntityDamageByEntityEvent) return

		when (event.getCause()) {
			EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
			EntityDamageEvent.DamageCause.ENTITY_EXPLOSION -> {
				val damagee = event.entity
				val damager = event.damager
				val cancel = FortressPlugin.forWorld(damagee.world).onEntityDamageFromExplosion(damagee, damager)
				if (cancel) event.isCancelled = true
			}
			else -> {}
		}
	}

	@EventHandler(ignoreCancelled = true)
	fun onEnderPearlThrown(event: PlayerTeleportEvent) {
		Log.log("onEnderPearlThrown() called")
		if (event.cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			val player = event.player
			val source = Point(event.from)
			val target = Point(event.to)
			val cancel = FortressPlugin.forWorld(event.to.world).onEnderPearlThrown(player, source, target)
			if (cancel) event.isCancelled = true
		}
	}
}