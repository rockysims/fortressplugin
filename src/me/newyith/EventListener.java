package me.newyith;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.ArrayList;

public class EventListener implements Listener {

    public EventListener(FortressPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        Block poweredBlock = event.getBlock();
        Point p = new Point(poweredBlock.getLocation());
        Point pBottom = new Point(p.world, p.x, p.y - 1, p.z);
        if (pBottom.matches(Material.OBSIDIAN) && p.matches(Material.REDSTONE_WIRE)) {
            ArrayList<Point> points = new ArrayList<Point>();
            points.add(new Point(p.world, p.x + 1, p.y, p.z + 0));
            points.add(new Point(p.world, p.x - 1, p.y, p.z + 0));
            points.add(new Point(p.world, p.x + 0, p.y, p.z + 1));
            points.add(new Point(p.world, p.x + 0, p.y, p.z - 1));
            for (Point point : points) {
                if (point.matches(Material.DIAMOND_BLOCK)) {
                    FortressGeneratorRunesManager.onPotentialRedstoneEvent(point.getBlock(), event.getNewCurrent());
                    break;
                }
            }
        }
    }
}
