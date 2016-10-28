package me.newyith.fortress.bedrock;

import com.google.common.collect.ImmutableMap;
import me.newyith.fortress.core.BedrockManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

public class BedrockManagerNewForWorld {
	private static class Model {
		private ImmutableMap<Point, Material> materialByPointMap;
		private Set<BedrockBatch> batches;
		private Set<Point> updatePoints;
		private String worldName;
		private transient World world;
		private final transient Object mutex;

		@JsonCreator
		public Model(@JsonProperty("materialByPointMap") ImmutableMap<Point, Material> materialByPointMap,
					 @JsonProperty("batches") Set<BedrockBatch> batches,
					 @JsonProperty("updatePoints") Set<Point> updatePoints,
					 @JsonProperty("worldName") String worldName) {
			this.materialByPointMap = materialByPointMap;
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
		model = new Model(ImmutableMap.of(), new HashSet<>(), new HashSet<>(), world.getName());
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
		Set<Point> shouldBeConverted;
		synchronized (model.mutex) {
			Debug.start("fillAllBatchPoints");
			for (BedrockBatch batch : model.batches) {
				allBatchPoints.addAll(batch.getPoints());
			}
			Debug.end("fillAllBatchPoints");

			shouldBeConverted = model.updatePoints.parallelStream()
					.filter(allBatchPoints::contains)
					.collect(Collectors.toSet());
		}

		Map<Point, Material> matByPoint = new HashMap<>(model.materialByPointMap);

		//update matByPoint and convert/revert as needed
		for (Point p : model.updatePoints) {
			if (shouldBeConverted.contains(p)) {
				//ensure converted
				Material mat = ensureConverted(p);
				matByPoint.put(p, mat);
			} else {
				//ensure reverted
				ensureReverted(p);
				matByPoint.remove(p);
			}
		}

		model.materialByPointMap = ImmutableMap.copyOf(matByPoint);
	}

	private Material ensureConverted(Point p) {
		BedrockManager.convert(model.world, p); //TODO: delete and replace this line
		return BedrockManager.getMaterial(model.world, p); //TODO: delete and replace this line
	}

	private void ensureReverted(Point p) {
		BedrockManager.fullRevert(model.world, p); //TODO: delete and replace this line
	}

	public Material getMaterial(Point p) {
		return model.materialByPointMap.get(p);
	}

	public Map<Point, Material> getMaterialByPointMap() {
		return model.materialByPointMap;
	}
}



//TimedBedrockManager::convertTimed(Set<Point> points)

//TODO: try to refactor to use batch converts
//	BedrockBatch batch = BedrockManager.convert(points);
//	batch.revert();