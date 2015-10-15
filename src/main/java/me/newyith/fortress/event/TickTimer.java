package me.newyith.fortress.event;

import me.newyith.fortress.command.Commands;
import me.newyith.fortress.generator.FortressGeneratorRunesManager;
import me.newyith.fortress.main.FortressPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TickTimer extends BukkitRunnable {
	public static final int msPerTick = 150; //should be divisible by 50

	public TickTimer() {

	}

	public static void onEnable(FortressPlugin plugin) {
		new TickTimer().runTaskTimer(plugin, 0, msPerTick / 50);
	}

	@Override
	public void run() {
//		Debug.start("tick");

		FortressGeneratorRunesManager.onTick();
		Commands.onTick();
		FortressPlugin.onTick();

//		Debug.stop("tick", false);
//		Debug.duration("tick");
//		Debug.clear("tick");
	}
}