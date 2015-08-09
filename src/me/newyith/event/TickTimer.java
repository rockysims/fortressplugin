package me.newyith.event;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TickTimer extends BukkitRunnable {
	public static int msPerTick = 150; //should be divisible by 50

	public TickTimer() {

	}

	public static void onEnable(FortressPlugin plugin) {
		new TickTimer().runTaskTimer(plugin, 0, msPerTick / 50);
	}

	@Override
	public void run() {
		FortressGeneratorRunesManager.onTick();
	}
}