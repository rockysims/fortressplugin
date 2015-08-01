package me.newyith.event;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
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
}
