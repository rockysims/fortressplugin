package me.newyith;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class FortressPlugin extends JavaPlugin {
    private static final Logger log = Logger.getLogger("FortressPluginLogger");

    @Override
    public void onEnable() {
        log.info("FortressPlugin onEnable called");
    }

    @Override
    public void onDisable() {
        log.info("FortressPlugin onDisable called");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("hello") && sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("Hello, " + player.getName() + ".");
            return true;
        }
        return false;
    }
}
