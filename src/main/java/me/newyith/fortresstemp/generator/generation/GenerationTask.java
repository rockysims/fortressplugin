package me.newyith.fortresstemp.generator.generation;

import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Point;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Observable;
import java.util.Set;

public class GenerationTask extends BukkitRunnable implements Observable {
	private List<Set<Point>> layers;
	private boolean isGenerating;
	private boolean animate = true;

	private final int maxBlocksPerFrame = 500;
	private final int ticksPerFrame = 150 / TickTimer.msPerTick; // msPerFrame / msPerTick
	private int waitTicks = 0;
	private int curIndex = 0;

	public GenerationTask(List<Set<Point>> layers, boolean isGenerating, boolean instant) {
		this.layers = layers;
		this.isGenerating = isGenerating;

		if (instant) {
			animate = false;
			onTick();
		} else {
			TickTimer.onTick(onTick());
		}
	}

	private void onTick() {

	}

	@Override
	public void run() {

	}
}
