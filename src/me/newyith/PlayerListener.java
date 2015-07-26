package me.newyith;

import org.bukkit.event.EventHandler;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener implements Listener {

    public PlayerListener(FortressPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            player.sendMessage("onPlayerRightClickBlock: " + ((Block)clickedBlock).getType().name());
        }
    }
}
