package me.newyith;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class FortressPlugin extends JavaPlugin {
    private static final Logger log = Logger.getLogger("FortressPluginLogger");

    @Override
    public void onEnable() {
        log.info("FortressPlugin onEnable called");
        new EventListener(this);
    }

    @Override
    public void onDisable() {
        log.info("FortressPlugin onDisable called");
    }
}

//TODO: make indicator blocks move to show fortress generator rune state
//TODO: allow piston events to break rune(s)
//TODO: work on fortress fuel (detecting it in chest and using it up and tracking how much fuel is currently burning)
//  TODO: work on handling fortress generator state (maybe time to look at FortressMod code?)