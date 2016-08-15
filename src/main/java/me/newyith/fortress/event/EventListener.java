package me.newyith.fortress.event;

import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Blocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//fully written again
public class EventListener implements Listener {
	public EventListener(FortressPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public static void onEnable(FortressPlugin plugin) {
		new EventListener(plugin);
	}

	// - - - //

	//ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" to the method
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		boolean cancel = FortressesManager.onSignChange(player, block);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		FortressesManager.onBlockBreakEvent(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnvironmentBreaksRedstoneWireEvent(BlockFromToEvent event) {
		if(event.getToBlock().getType() == Material.REDSTONE_WIRE) {
			FortressesManager.onEnvironmentBreaksRedstoneWireEvent(event.getToBlock());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		FortressesManager.onBlockRedstoneEvent(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block placedBlock = event.getBlockPlaced();
		Material replacedMaterial = event.getBlockReplacedState().getType();
		boolean cancel = FortressesManager.onBlockPlaceEvent(player, placedBlock, replacedMaterial);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
		World w = event.getBlock().getWorld();
		Point p = new Point(event.getBlock());

		BlockFace d = event.getDirection();
		int x = d.getModX();
		int y = d.getModY();
		int z = d.getModZ();
		Point t = p.add(x, y, z);

		Set<Block> movedBlocks = new HashSet<>(event.getBlocks());
		boolean isSticky = event.isSticky();

		boolean cancel = FortressesManager.onPistonEvent(isSticky, w, p, t, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		World w = event.getBlock().getWorld();
		Point p = new Point(event.getBlock());
		boolean isSticky = event.isSticky();
		Set<Block> movedBlocks = new HashSet<>(event.getBlocks());

		boolean cancel = FortressesManager.onPistonEvent(isSticky, w, p, null, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent event) {
//		Debug.msg("EventListener::onExplode(EntityExplodeEvent event) called");
		List<Block> explodeBlocks = event.blockList();
		Location loc = event.getLocation();
		//*
		float yield = event.getYield();
		/*/
		float yield = 3.0f;
		if (event.getEntity() instanceof Explosive) {
			Explosive e = (Explosive) event.getEntity();
			yield = e.getYield();
		} else if (event.getEntity() instanceof Creeper) {
			Creeper c = (Creeper) event.getEntity();
			if(c.isPowered()) {
				yield = 6.0f;
			} else {
				yield = 3.0f;
			}
		}
		Debug.msg("calculated yield: " + yield);
		//*/

		boolean cancel = FortressesManager.onExplode(explodeBlocks, loc);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		boolean cancel = FortressesManager.onIgnite(event.getBlock());
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		boolean cancel = FortressesManager.onBurn(event.getBlock());
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Action action = event.getAction();
		Block clicked = event.getClickedBlock();

		if (action == Action.RIGHT_CLICK_BLOCK) {
			if (Blocks.isDoor(clicked.getType())) {
				FortressesManager.onPlayerOpenCloseDoor(event);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRightClickBlock(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();
			FortressesManager.onPlayerRightClickBlock(player, block);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public boolean onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof Enderman)) return false; //if enderman
		boolean cancel = false;

		if (event.getTo() == Material.AIR) {
			//picking up block
			cancel = FortressesManager.onEndermanPickupBlock(event.getBlock());
			if (cancel) {
				event.setCancelled(true);
			}
		}

		return cancel;
	}



	Set<Player> playersExitingVehicle = new HashSet<>();

	@EventHandler(ignoreCancelled = true)
	public void onExitVehicle(VehicleExitEvent event) {
		if (!(event.getExited() instanceof Player)) return; //if player

		//holding shift to exit vehicle calls this event many times very fast
		//so wait for handler method to finish before allowing it to be called again (per player)
		Player player = (Player) event.getExited();
		if (!playersExitingVehicle.contains(player)) {
			playersExitingVehicle.add(player);
			FortressesManager.onPlayerExitVehicle(player);
			playersExitingVehicle.remove(player);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageFromExplosion(EntityDamageEvent event) {
		if (!(event instanceof EntityDamageByEntityEvent)) return;

		EntityDamageEvent.DamageCause cause = event.getCause();
		if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
			Entity damagee = e.getEntity();
			Entity damager = e.getDamager();
			boolean cancel = FortressesManager.onEntityDamageFromExplosion(damagee, damager);
			if (cancel) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnderPearlThrown(PlayerTeleportEvent event) {
		if (!event.isCancelled() && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			Player player = event.getPlayer();
			Point source = new Point(event.getFrom());
			Point target = new Point(event.getTo());
			boolean cancel = FortressesManager.onEnderPearlThrown(player, source, target);
			if (cancel) {
				event.setCancelled(true);
			}
		}
	}
}
