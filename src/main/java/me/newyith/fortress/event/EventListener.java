package me.newyith.fortress.event;

import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Blocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;

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





	//TODO: delete or use this method (need to think about how saving will work)
	@EventHandler
	public void onWorldSave(WorldSaveEvent e) {
		Debug.msg("onWorldSave: " + e.getWorld().getName());
	}







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
		Point p = new Point(event.getBlock());

		BlockFace d = event.getDirection();
		int x = d.getModX();
		int y = d.getModY();
		int z = d.getModZ();
		Point t = p.add(x, y, z);

		Set<Block> movedBlocks = new HashSet<>(event.getBlocks());
		boolean isSticky = event.isSticky();

		boolean cancel = FortressesManager.onPistonEvent(isSticky, p, t, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		Point p = new Point(event.getBlock());
		boolean isSticky = event.isSticky();
		Set<Block> movedBlocks = new HashSet<>(event.getBlocks());

		boolean cancel = FortressesManager.onPistonEvent(isSticky, p, null, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent event) {
		List<Block> explodeBlocks = event.blockList();
		Location loc = event.getLocation();
		float yield = event.getYield();

		boolean cancel = FortressesManager.onExplode(explodeBlocks, loc, yield);
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
}
