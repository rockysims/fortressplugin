package me.newyith.fortress.event;

import me.newyith.fortress.bedrock.BedrockManagerNew;
import me.newyith.fortress.bedrock.timed.TimedBedrockManagerNew;
import me.newyith.fortress.command.Commands;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
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
		TimedBedrockManagerNew.onTick();
		BedrockManagerNew.onTick();
		Commands.onTick();
		FortressPlugin.onTick();
//		Debug.end("tick");
	}
}