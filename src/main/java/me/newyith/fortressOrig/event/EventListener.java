package me.newyith.fortressOrig.event;

import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.main.FortressesManager;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
		World world = block.getWorld();
		boolean cancel = FortressesManager.forWorld(world).onSignChange(player, block);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		World world = event.getBlock().getWorld();
		FortressesManager.forWorld(world).onBlockBreakEvent(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnvironmentBreaksRedstoneWireEvent(BlockFromToEvent event) {
		if (event.getToBlock().getType() == Material.REDSTONE_WIRE) {
			World world = event.getToBlock().getWorld();
			FortressesManager.forWorld(world).onEnvironmentBreaksRedstoneWireEvent(event.getToBlock());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		World world = event.getBlock().getWorld();
		FortressesManager.forWorld(world).onBlockRedstoneEvent(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Block placedBlock = event.getBlockPlaced();
		World world = placedBlock.getWorld();
		Player player = event.getPlayer();
		Material replacedMaterial = event.getBlockReplacedState().getType();
		boolean cancel = FortressesManager.forWorld(world).onBlockPlaceEvent(player, placedBlock, replacedMaterial);
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

		boolean cancel = FortressesManager.forWorld(w).onPistonEvent(isSticky, p, t, movedBlocks);
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

		boolean cancel = FortressesManager.forWorld(w).onPistonEvent(isSticky, p, null, movedBlocks);
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

		World world = loc.getWorld();
		boolean cancel = FortressesManager.forWorld(world).onExplode(explodeBlocks, loc);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		World world = event.getBlock().getWorld();
		boolean cancel = FortressesManager.forWorld(world).onIgnite(event.getBlock());
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		World world = event.getBlock().getWorld();
		boolean cancel = FortressesManager.forWorld(world).onBurn(event.getBlock());
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action == Action.RIGHT_CLICK_BLOCK) {
			Block clicked = event.getClickedBlock();
			World world = clicked.getWorld();

			Player player = event.getPlayer();
			BlockFace face = event.getBlockFace();
			FortressesManager.forWorld(world).onPlayerRightClickBlock(player, clicked, face);

			if (Blocks.isDoor(clicked.getType())) {
				boolean cancel = FortressesManager.forWorld(world).onPlayerOpenCloseDoor(player, clicked);
				if (cancel) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public boolean onEntityChangeBlock(EntityChangeBlockEvent event) {
		boolean cancel = false;

		Entity entity = event.getEntity();
		if (entity instanceof Enderman) {
			if (event.getTo() == Material.AIR) {
				//picking up block
				Block block = event.getBlock();
				World world = block.getWorld();
				cancel = FortressesManager.forWorld(world).onEndermanPickupBlock(block);
				if (cancel) {
					event.setCancelled(true);
				}
			}
		} else if (entity instanceof Zombie) {
			if (event.getTo() == Material.AIR) {
				Block block = event.getBlock();
				World world = block.getWorld();
				cancel = FortressesManager.forWorld(world).onZombieBreakBlock(block);
				if (cancel) {
					event.setCancelled(true);
				}
			}
		}

		return cancel;
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		HumanEntity humanEntity = event.getPlayer();
		if (humanEntity instanceof Player) {
			Inventory inventory = event.getInventory();
			InventoryHolder holder = inventory.getHolder();
			if (holder instanceof Chest) { //Trap Chest is also instanceof Chest
				Chest chest = (Chest) holder;
				Block block = chest.getBlock();
				Player player = (Player) humanEntity;
				World world = block.getWorld();
				FortressesManager.forWorld(world).onPlayerCloseChest(player, block);
			}
		}
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
			World world = player.getWorld();
			FortressesManager.forWorld(world).onPlayerExitVehicle(player);
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
			World world = damagee.getWorld();
			boolean cancel = FortressesManager.forWorld(world).onEntityDamageFromExplosion(damagee, damager);
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
			World world = event.getTo().getWorld();
			boolean cancel = FortressesManager.forWorld(world).onEnderPearlThrown(player, source, target);
			if (cancel) {
				event.setCancelled(true);
			}
		}
	}
}
