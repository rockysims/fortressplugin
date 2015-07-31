package me.newyith.main;

import me.newyith.memory.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.logging.Logger;

public class FortressPlugin extends JavaPlugin {
    private static final Logger log = Logger.getLogger("FortressPluginLogger");
    //log.info("FortressPlugin onDisable called");

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        new EventListener(this);
        configManager = new ConfigManager(this);
        configManager.onEnable();

        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
        sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> ON <<           ", ChatColor.GREEN);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    @Override
    public void onDisable() {
        configManager.onDisable();

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

//TODO: make indicator blocks move to show fortress generator rune state
//    done except for Needs Fuel state
//TODO: work on fortress fuel (detecting it in chest and using it up and tracking how much fuel is currently burning)
//TODO: allow piston events to break rune(s)
//  TODO: work on handling fortress generator state (maybe time to look at FortressMod code?)
//TODO: test adding particles

/* New Feature:
make pistons transmit generation when extended
    this will serve as a switch to allow nearby buildings to connect/disconnect from fortress generation
    pistons should have particles to indicate when the piston has been found by a fortress generator (onGeneratorStart searches)
    pistons should not be protected (breakable)
//*/