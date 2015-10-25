package me.newyith.fortresstemp.generator.manager;

import me.newyith.fortressold.main.FortressPlugin;
import org.bukkit.event.Listener;

public class EventListener implements Listener {

	public static void onEnable(FortressPlugin plugin) {
		new EventListener(plugin);
	}

	public EventListener(FortressPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	/*
	@EventHandler(ignoreCancelled = true) //ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" to the method
	public void onBlockBreakEvent(BlockBreakEvent event) {
		FortressPlugin.generatorRunesManager.onBlockBreakEvent(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		boolean cancel = FortressPlugin.generatorRunesManager.onSignChange(player, block);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnvironmentBreaksRedstoneWireEvent(BlockFromToEvent event) {
		if(event.getToBlock().getType() == Material.REDSTONE_WIRE) {
			FortressPlugin.generatorRunesManager.onWaterBreaksRedstoneWireEvent(event.getToBlock());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block placedBlock = event.getBlockPlaced();
		boolean cancel = FortressPlugin.generatorRunesManager.onBlockPlaceEvent(player, placedBlock);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		FortressPlugin.generatorRunesManager.onBlockRedstoneEvent(event);
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

		boolean cancel = FortressPlugin.generatorRunesManager.onPistonEvent(isSticky, p, t, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		Point p = new Point(event.getBlock().getLocation());
		boolean isSticky = event.isSticky();
		ArrayList<Block> movedBlocks = new ArrayList<>(event.getBlocks());

		boolean cancel = FortressPlugin.generatorRunesManager.onPistonEvent(isSticky, p, null, movedBlocks);
		if (cancel) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent event) {
		FortressPlugin.generatorRunesManager.onExplode(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Action action = event.getAction();
		Block clicked = event.getClickedBlock();

		if (action == Action.RIGHT_CLICK_BLOCK) {
			if (Wall.isDoor(clicked.getType())) {
				FortressPlugin.generatorRunesManager.onPlayerOpenCloseDoor(event);
			}
		}
	}
	*/
}
