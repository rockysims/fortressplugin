package me.newyith.fortress.event;

import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;

public class EventListener implements Listener {

	public EventListener(FortressPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public static void onEnable(FortressPlugin plugin) {
		new EventListener(plugin);
	}

	// - - - //

	//*

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
			FortressesManager.onWaterBreaksRedstoneWireEvent(event.getToBlock());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		FortressesManager.onBlockRedstoneEvent(event);
	}
	//*/








/*
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
		Point p = new Point(event.getBlock().getLocation());

		BlockFace d = event.getDirection();
		int x = d.getModX();
		int y = d.getModY();
		int z = d.getModZ();
		Point t = new Point(p.world, p.x + x, p.y + y, p.z + z);

		ArrayList<Block> movedBlocks = new ArrayList<>(event.getBlocks());

		boolean isSticky = event.isSticky();

		boolean cancel = FortressesManager.onPistonEvent(isSticky, p, t, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		Point p = new Point(event.getBlock().getLocation());
		boolean isSticky = event.isSticky();
		ArrayList<Block> movedBlocks = new ArrayList<>(event.getBlocks());

		boolean cancel = FortressesManager.onPistonEvent(isSticky, p, null, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent event) {
		FortressesManager.onExplode(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Action action = event.getAction();
		Block clicked = event.getClickedBlock();

		if (action == Action.RIGHT_CLICK_BLOCK) {
			if (Wall.isDoor(clicked.getType())) {
				FortressesManager.onPlayerOpenCloseDoor(event);
			}
		}
	}
//*/
}
