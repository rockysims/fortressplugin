package me.newyith.fortress.bedrock.timed;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockManagerNew;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public class TimedBedrockManagerNewForWorld {
	private static class Model {
		private PriorityQueue<TimedBedrockBatch> timedBedrockBatches;
		private int curTick;
		private final String worldName;
		private final transient World world;
		private final transient Random random;

		@JsonCreator
		public Model(@JsonProperty("timedBedrockBatches") PriorityQueue<TimedBedrockBatch> timedBedrockBatches,
					 @JsonProperty("curTick") int curTick,
					 @JsonProperty("worldName") String worldName) {
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
	public TimedBedrockManagerNewForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockManagerNewForWorld(World world) {
		model = new Model(new PriorityQueue<>(), 0, world.getName());
	}

	//-----------------------------------------------------------------------

	public void onTick() {
		model.curTick++;
		revertExpiredBedrock();
	}

	public void convert(BedrockAuthToken authToken, Set<Point> points) {
		int msDuration = 500 + model.random.nextInt(750);
		convert(authToken, points, msDuration);
	}

	public void convert(BedrockAuthToken authToken, Set<Point> points, int msDuration) {
		int tickDuration = msDuration / TickTimer.msPerTick;
		int endTick = model.curTick + tickDuration;
		TimedBedrockBatch timedBedrockBatch = new TimedBedrockBatch(authToken, points, endTick);
		BedrockManagerNew.forWorld(model.world).convert(timedBedrockBatch);
		model.timedBedrockBatches.add(timedBedrockBatch);
	}

	private void revertExpiredBedrock() {
		TimedBedrockBatch timedBedrockBatch = model.timedBedrockBatches.peek();
		while (timedBedrockBatch != null && isExpired(timedBedrockBatch)) {
			model.timedBedrockBatches.remove(timedBedrockBatch);
			BedrockManagerNew.forWorld(model.world).revert(timedBedrockBatch);
			timedBedrockBatch.destroy();
			timedBedrockBatch = model.timedBedrockBatches.peek();
		}
	}

	private boolean isExpired(TimedBedrockBatch timedBedrockBatch) {
		return model.curTick > timedBedrockBatch.getEndTick();
	}
}
