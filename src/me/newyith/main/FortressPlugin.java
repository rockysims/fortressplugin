package me.newyith.main;

import me.newyith.event.EventListener;
import me.newyith.event.TickTimer;
import me.newyith.memory.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;

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



    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("test")) {
            long now = (new Date()).getTime();
            Bukkit.broadcastMessage("/test. now: " + now);
            Bukkit.broadcastMessage("/test. (new Date()): " + (new Date()));
            return true;
        }
        return false;
    }



}

/*
Particles:
TODO: refactor how particles work to make it more efficient

TODO: tidy up from adding layerOutsideFortress
TODO: display protection particles at all points in layerOutsideFortress (need to rename to generation particles)
	will need to make layerOutsideFortress a Map<Point, Point> so particles can be displayed on surface of correct face?
		no because of what to do if multiple surfaces should have particles
		instead maybe do wallPoints.contains(each of the 6 points next to point in layerOutsideFortress)?
			yes but not every time. Map<Point, Set<Point>>

//TODO: refactor to use the listener pattern?


TODO: make generation display wave of particles to indicate generating protected blocks

*/

//TODO: in Wall class and other places its used: rename wallMaterials to traverseMaterials
//TODO: consider making rune activation require an empty hand
//TODO: test adding particles
//TODO: test killing the server (ctrl+c not "stop") and make sure plugin is robust enough to handle it
//  TODO: work on handling fortress generator state (maybe time to look at FortressMod code?)
//TODO: make glowstone blocks work as fuel for 4x the fuel value of glowstone dust (silk touch works on glowstone block and fortune III does not)
//TODO: consider making creating rune require empty hand (again)

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