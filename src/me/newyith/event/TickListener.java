package me.newyith.event;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TickListener extends BukkitRunnable {
	public TickListener() {

	}

	public static void onEnable(FortressPlugin plugin) {
		new TickListener().runTaskTimer(plugin, 0, 5); //run 20 times per second
	}

	@Override
	public void run() {
		FortressGeneratorRunesManager.onTick();
	}
}
