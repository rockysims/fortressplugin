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
		//TODO:: delete all uses of Debug in this method
		Debug.queuePrints = true;

		Debug.start("tick");

		Debug.start("FortressesManager.onTick()");
		FortressesManager.onTick();
		Debug.end("FortressesManager.onTick()");

		Debug.start("TimedBedrockManager.onTick()");
		TimedBedrockManager.onTick();
		Debug.end("TimedBedrockManager.onTick()");

		Debug.start("BedrockManager.onTick()");
		BedrockManager.onTick();
		Debug.end("BedrockManager.onTick()");

		Debug.start("Commands.onTick()");
		Commands.onTick();
		Debug.end("Commands.onTick()");

		Debug.start("FortressPlugin.onTick()");
		FortressPlugin.onTick();
		Debug.end("FortressPlugin.onTick()");

		Debug.stop("tick", false);
		double duration = Debug.duration("tick");
		Debug.end("tick");

		Debug.flushQueuedPrints(duration > 10);
		Debug.queuePrints = false;
	}
}