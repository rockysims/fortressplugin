package me.newyith;

import org.bukkit.Bukkit;
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

public class EventListener implements Listener {

    public EventListener(FortressPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            FortressGeneratorRune.onPlayerRightClickBlock(player, clickedBlock);
        }
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        FortressGeneratorRune.onBlockBreakEvent(event.getBlock());
    }

    @EventHandler
    public void onWaterBreaksRedstoneWireEvent(BlockFromToEvent event) {
        if(event.getToBlock().getType() == Material.REDSTONE_WIRE) {
            FortressGeneratorRune.onWaterBreaksRedstoneWireEvent(event.getToBlock());
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        FortressGeneratorRune.onBlockPlaceEvent(event.getBlock());
    }
}
