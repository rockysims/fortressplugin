package me.newyith.fortress.bedrock.timed;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public class TimedBedrockManagerForWorld {
	private static class Model {
		private PriorityQueue<TimedForceReversionBatch> timedForceReversionBatches;
		private PriorityQueue<TimedBedrockBatch> timedBedrockBatches;
		private int curTick;
		private final String worldName;
		private final transient World world;
		private final transient Random random;

		@JsonCreator
		public Model(@JsonProperty("timedForceReversionBatches") PriorityQueue<TimedForceReversionBatch> timedForceReversionBatches,
					 @JsonProperty("timedBedrockBatches") PriorityQueue<TimedBedrockBatch> timedBedrockBatches,
					 @JsonProperty("curTick") int curTick,
					 @JsonProperty("worldName") String worldName) {
			this.timedForceReversionBatches = timedForceReversionBatches;
			this.timedBedrockBatches = timedBedrockBatches;
			this.curTick = curTick;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.random = new Random();
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockManagerForWorld(World world) {
		model = new Model(new PriorityQueue<>(), new PriorityQueue<>(), 0, world.getName());
	}

	//-----------------------------------------------------------------------

	public void onTick() {
		model.curTick++;
		revertExpiredBedrock();
		removeExpiredForceReversions();
	}

	//---

	public void convert(BedrockAuthToken authToken, Set<Point> points) {
		int msDuration = 500 + model.random.nextInt(750);
		convert(authToken, points, msDuration);
	}

	public void convert(BedrockAuthToken authToken, Set<Point> points, int msDuration) {
		int tickDuration = msDuration / TickTimer.msPerTick;
		int endTick = model.curTick + tickDuration;
		TimedBedrockBatch timedBedrockBatch = new TimedBedrockBatch(authToken, points, endTick);
		BedrockManager.forWorld(model.world).convert(timedBedrockBatch);
		model.timedBedrockBatches.add(timedBedrockBatch);
	}

	private void revertExpiredBedrock() {
		TimedBedrockBatch timedBedrockBatch = model.timedBedrockBatches.peek();
		while (timedBedrockBatch != null && isExpired(timedBedrockBatch)) {
			model.timedBedrockBatches.remove(timedBedrockBatch);
			BedrockManager.forWorld(model.world).revert(timedBedrockBatch);
			timedBedrockBatch.destroy();
			timedBedrockBatch = model.timedBedrockBatches.peek();
		}
	}
	private boolean isExpired(TimedBedrockBatch timedBedrockBatch) {
		return model.curTick > timedBedrockBatch.getEndTick();
	}

	//---

	public void forceReversion(BedrockAuthToken authToken, Set<Point> points, int msDuration) {
		int tickDuration = msDuration / TickTimer.msPerTick;
		int endTick = model.curTick + tickDuration;
		TimedForceReversionBatch timedForceReversionBatch = new TimedForceReversionBatch(authToken, points, endTick);
		BedrockManager.forWorld(model.world).addForceReversion(timedForceReversionBatch);
		model.timedForceReversionBatches.add(timedForceReversionBatch);
	}

	private void removeExpiredForceReversions() {
		TimedForceReversionBatch timedForceReversionBatch = model.timedForceReversionBatches.peek();
		while (timedForceReversionBatch != null && isExpired(timedForceReversionBatch)) {
			model.timedForceReversionBatches.remove(timedForceReversionBatch);
			BedrockManager.forWorld(model.world).removeForceReversion(timedForceReversionBatch);
			timedForceReversionBatch.destroy();
			timedForceReversionBatch = model.timedForceReversionBatches.peek();
		}
	}
	private boolean isExpired(TimedForceReversionBatch timedForceReversionBatch) {
		return model.curTick > timedForceReversionBatch.getEndTick();
	}
}
