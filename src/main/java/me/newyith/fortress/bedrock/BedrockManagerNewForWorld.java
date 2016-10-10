package me.newyith.fortress.bedrock;

import me.newyith.fortress.core.BedrockManager;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BedrockManagerNewForWorld {
	private static class Model {
		private Set<BedrockBatch> batches;
		private Set<Point> updatePoints;
		private String worldName;
		private transient World world;
		private final transient Object mutex;

		@JsonCreator
		public Model(@JsonProperty("batches") Set<BedrockBatch> batches,
					 @JsonProperty("updatePoints") Set<Point> updatePoints,
					 @JsonProperty("worldName") String worldName) {
			this.batches = batches;
			this.updatePoints = updatePoints;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.mutex = new Object();
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockManagerNewForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManagerNewForWorld(World world) {
		model = new Model(new HashSet<>(), new HashSet<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	//this method is (or should be) thread safe
	public BedrockBatch convert(Set<Point> points) {
		BedrockBatch batch = new BedrockBatch(points);
		synchronized (model.mutex) {
			model.batches.add(batch);
			model.updatePoints.addAll(batch.getPoints());
		}

		return batch;
	}

	//this method is (or should be) thread safe
	public void revert(BedrockBatch batch) {
		synchronized (model.mutex) {
			model.batches.remove(batch);
			model.updatePoints.addAll(batch.getPoints());
		}
	}

	public void onTick() {
		if (model.updatePoints.size() > 0) {
			update();
			model.updatePoints.clear();
		}
	}

	private void update() {
		Set<Point> allBatchPoints = new HashSet<>();
		Set<Point> convertPoints;
		synchronized (model.mutex) {
			for (BedrockBatch batch : model.batches) {
				allBatchPoints.addAll(batch.getPoints());
			}

			convertPoints = model.updatePoints.parallelStream()
					.filter(allBatchPoints::contains)
					.collect(Collectors.toSet());
		}


		/*
		//TODO: ensure convertPoints are converted
		//TODO: ensure points not in convertPoints are reverted
		/*/
		//call old BedrockManager for now
		for (Point p : model.updatePoints) {
			if (convertPoints.contains(p)) {
				//ensure converted
				BedrockManager.convert(model.world, p);
			} else {
				//ensure reverted
				BedrockManager.fullRevert(model.world, p);
			}
		}
		//*/
	}


//BedrockManager::convert(Set<Point> points) //maybe return batch object can then later call batch.revert()
//BedrockManager::revert(Set<Point> points) //batch.revert() instead
//BedrockManager::convertTimed(Set<Point> points)

//TODO: try to refactor to use batch converts
//	BedrockBatch batch = BedrockManager.convert(points);
//	batch.revert();
}
