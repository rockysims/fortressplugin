package me.newyith.fortress.event;

import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.command.Commands;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Debug;
import org.bukkit.scheduler.BukkitRunnable;

public class TickTimer extends BukkitRunnable {
	public static final int msPerTick = 150; //should be divisible by 50
	//tick internally only every 3rd game tick in hopes it will improve performance

	public static void onEnable(FortressPlugin plugin) {
		new TickTimer().runTaskTimer(plugin, 0, msPerTick / 50);
	}

	@Override
	public void run() {
		FortressesManager.onTick();
		TimedBedrockManager.onTick();
		BedrockManager.onTick();
		Commands.onTick();
		FortressPlugin.onTick();
	}
}