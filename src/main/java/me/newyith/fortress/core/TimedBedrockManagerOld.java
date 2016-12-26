package me.newyith.fortress.core;

import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Point;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.PriorityQueue;
import java.util.Random;

public class TimedBedrockManagerOld {
	private static TimedBedrockManagerOld instance = null;
	public static TimedBedrockManagerOld getInstance() {
		if (instance == null) {
			instance = new TimedBedrockManagerOld();
		}
		return instance;
	}
	public static void setInstance(TimedBedrockManagerOld newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private PriorityQueue<TimedBedrockOld> timedBedrocks = null;
		private int curTick = 0;
		private transient final Random random;

		@JsonCreator
		public Model(@JsonProperty("timedBedrocks") PriorityQueue<TimedBedrockOld> timedBedrocks,
					 @JsonProperty("curTick") int curTick) {
			this.timedBedrocks = timedBedrocks;
			this.curTick = curTick;

			//rebuild transient fields
			this.random = new Random();
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockManagerOld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockManagerOld() {
		model = new Model(new PriorityQueue<>(), 0);
	}

	//-----------------------------------------------------------------------

	public static void onTick() {
		instance.model.curTick++;
		instance.revertExpiredBedrock();
	}

	public static void convert(World w, Point p, int msDuration) {
		instance.doConvert(w, p, msDuration);
	}
	public static void convert(World w, Point p) {
		int msDuration = 500 + instance.model.random.nextInt(750);
		instance.doConvert(w, p, msDuration);
	}
	public void doConvert(World w, Point p, int msDuration) {
		int tickDuration = msDuration / TickTimer.msPerTick;
		int endTick = model.curTick + tickDuration;
		TimedBedrockOld timedBedrock = new TimedBedrockOld(w, p, endTick);
		timedBedrock.convert();
		model.timedBedrocks.add(timedBedrock);
	}

	private void revertExpiredBedrock() {
		TimedBedrockOld timedBedrock = model.timedBedrocks.peek();
		while (timedBedrock != null && isExpired(timedBedrock)) {
			model.timedBedrocks.remove(timedBedrock);
			timedBedrock.revert();
			timedBedrock = model.timedBedrocks.peek();
		}
	}

	private boolean isExpired(TimedBedrockOld timedBedrock) {
		return model.curTick > timedBedrock.getEndTick();
	}
}
