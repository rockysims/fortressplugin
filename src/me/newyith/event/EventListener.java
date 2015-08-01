package me.newyith.event;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;

public class EventListener implements Listener {

    public EventListener(FortressPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void onEnable(FortressPlugin plugin) {
        new EventListener(plugin);
    }

    @EventHandler
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            FortressGeneratorRunesManager.onPlayerRightClickBlock(player, clickedBlock);
        }
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        FortressGeneratorRunesManager.onBlockBreakEvent(event.getBlock());
    }

    @EventHandler
    public void onWaterBreaksRedstoneWireEvent(BlockFromToEvent event) {
        if(event.getToBlock().getType() == Material.REDSTONE_WIRE) {
            FortressGeneratorRunesManager.onWaterBreaksRedstoneWireEvent(event.getToBlock());
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        FortressGeneratorRunesManager.onBlockPlaceEvent(event.getBlock());
    }

    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
        FortressGeneratorRunesManager.onBlockRedstoneEvent(event.getBlock(), event.getNewCurrent());
    }

    @EventHandler
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
        if (!event.isSticky()) {
            Point p = new Point(event.getBlock().getLocation());

			BlockFace d = event.getDirection();
			int x = d.getModX();
			int y = d.getModY();
			int z = d.getModZ();
			Point t = new Point(p.world, p.x + x, p.y + y, p.z + z);

			ArrayList<Block> pushed = new ArrayList<>(event.getBlocks());

            FortressGeneratorRunesManager.onNonStickyPistonEvent(p, t, pushed);
        }
    }

    @EventHandler
    public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		if (!event.isSticky()) {
			Point p = new Point(event.getBlock().getLocation());
			FortressGeneratorRunesManager.onNonStickyPistonEvent(p, null, null);
		}
    }
}
