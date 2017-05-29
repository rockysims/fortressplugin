package me.newyith.fortressOrig.event;

import me.newyith.fortressOrig.bedrock.BedrockManager;
import me.newyith.fortressOrig.bedrock.timed.TimedBedrockManager;
import me.newyith.fortressOrig.command.Commands;
import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.main.FortressesManager;
import org.bukkit.scheduler.BukkitRunnable;

public class TickTimer extends BukkitRunnable {
	public static final int msPerTick = 150; //should be divisible by 50
	//tick internally only every 3rd game tick in hopes it will improve performance

	public static void onEnable(FortressPlugin plugin) {
		new TickTimer().runTaskTimer(plugin, 0, msPerTick / 50);
	}

	@Override
	public void run() {
//		Debug.start("tick");
		FortressesManager.onTick();
		TimedBedrockManager.onTick();
		BedrockManager.onTick();
		Commands.onTick();
		FortressPlugin.onTick();
//		Debug.end("tick");
	}
}