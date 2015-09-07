package me.newyith.event;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.util.Wall;
import me.newyith.main.FortressPlugin;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;

public class EventListener implements Listener {

    public EventListener(FortressPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void onEnable(FortressPlugin plugin) {
        new EventListener(plugin);
    }

    @EventHandler(ignoreCancelled = true) //ignoreCancelled adds a virtual "if (event.isCancelled()) { return; }" to the method
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			Block clickedBlock = event.getClickedBlock();
            FortressGeneratorRunesManager.onPlayerRightClickBlock(player, clickedBlock);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        FortressGeneratorRunesManager.onBlockBreakEvent(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnvironmentBreaksRedstoneWireEvent(BlockFromToEvent event) {
        if(event.getToBlock().getType() == Material.REDSTONE_WIRE) {
            FortressGeneratorRunesManager.onWaterBreaksRedstoneWireEvent(event.getToBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        FortressGeneratorRunesManager.onBlockPlaceEvent(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
        FortressGeneratorRunesManager.onBlockRedstoneEvent(event);
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

		FortressGeneratorRunesManager.onPistonEvent(isSticky, p, t, movedBlocks);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		Point p = new Point(event.getBlock().getLocation());
		boolean isSticky = event.isSticky();
		ArrayList<Block> movedBlocks = new ArrayList<>(event.getBlocks());
		FortressGeneratorRunesManager.onPistonEvent(isSticky, p, null, movedBlocks);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
		FortressGeneratorRunesManager.onExplode(event.blockList());
    }

    @EventHandler
    public void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
        Action action = event.getAction();
        Block clicked = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK) {
			if (Wall.isDoor(clicked.getType())) {
				FortressGeneratorRunesManager.onPlayerOpenCloseDoor(event);
			}
        }
    }
}
