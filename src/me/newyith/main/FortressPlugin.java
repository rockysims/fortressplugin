package me.newyith.main;

import me.newyith.event.EventListener;
import me.newyith.event.TickTimer;
import me.newyith.memory.ConfigManager;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FortressPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
		TickTimer.onEnable(this);
        EventListener.onEnable(this);
        ConfigManager.onEnable(this);

        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
        sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> ON <<           ", ChatColor.GREEN);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    @Override
    public void onDisable() {
        ConfigManager.onDisable(this);

		sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
		sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> OFF <<          ", ChatColor.RED);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    private void sendToConsole(String s, ChatColor color) {
        ConsoleCommandSender console = this.getServer().getConsoleSender();
        console.sendMessage(color + s);
    }


	//TODO: remove this command
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("test")) {
			if (sender instanceof Player) {
				Debug.msg("executing test command...");

				int distance = 20;
				Player player = (Player)sender;
				Point center = new Point(player.getLocation());

				for (int xOffset = -1 * distance; xOffset <= distance; xOffset++) {
					for (int yOffset = -1 * distance; yOffset <= distance; yOffset++) {
						for (int zOffset = -1 * distance; zOffset <= distance; zOffset++) {
							Point p = new Point(center.world, center.x + xOffset, center.y + yOffset, center.z + zOffset);
							if (p.y > 5) {
								if (p.getBlock().getType() == Material.BEDROCK) {
									p.getBlock().setType(Material.COBBLESTONE);
								}
							}
						}
					}
				}
			}

            return true;
        }
        return false;
    }



}


//TODO: do door white lists next and then pistons?



//TODO: get door white lists working
//TODO: make it so cycling generator always degenerates (instantly) all generated points that are now disconnected
//TODO: make glowstone blocks work as fuel for 4x the fuel value of glowstone dust (silk touch works on glowstone block and fortune III does not)
//TODO: add manual book (obsidian + book)
//TODO: make sure generators continue to burn fuel when no player is nearby
//TODO: add /stuck command

//low priority:
//TODO: refactor to use the listener pattern?
//TODO: in Wall class and other places its used: rename wallMaterials to traverseMaterials
//TODO: test killing the server (ctrl+c not "stop") and make sure plugin is robust enough to handle it
//TODO: consider making mossy cobblestone be generated but not transmit generation to anything except mossy
//TODO: consider making rune activation require an empty hand
//TODO: consider making creating rune require empty hand (again)
//TODO: make generation display wave of particles to indicate generating wall blocks?

/* New Feature:
make pistons transmit generation when extended
    this will serve as a switch to allow nearby buildings to connect/disconnect from fortress generation
    pistons should have particles to indicate when the piston has been found by a fortress generator (onGeneratorStart searches)
    pistons should not be protected (breakable)
//*/










//		int taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
//			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
//				Point point = new Point(player.getLocation().add(0, 2, 0));
//				float speed = 1;
//				int amount = 1;
//				double range = 10;
//				ParticleEffect.PORTAL.display(0, 0, 0, speed, amount, point, range);
//				Bukkit.broadcastMessage("display portal at " + point);
//			}
//		}, 0, 20); //20 ticks per second
//		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
//			//
//			Bukkit.getServer().getScheduler().cancelTask(taskId);
//			Bukkit.broadcastMessage("canceling taskId: " + taskId);
//		}, 20*120);