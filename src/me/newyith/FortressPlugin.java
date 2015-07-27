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

//TODO: allow piston events to break rune(s)
//TODO: make indicator blocks move to show fortress generator rune state