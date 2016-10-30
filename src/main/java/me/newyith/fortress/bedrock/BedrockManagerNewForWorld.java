package me.newyith.fortress.bedrock;

import com.google.common.collect.ImmutableMap;
import javafx.util.Pair;
import me.newyith.fortress.util.Blocks;
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
		private final BedrockHandler bedrockHandler;
		private ImmutableMap<Point, Material> materialByPoint;
		private final Set<BedrockBatch> batches;
		private final Set<Point> updatePoints;
		private final String worldName;
		private final transient World world;
		private final transient Object mutex;

		@JsonCreator
		public Model(@JsonProperty("bedrockHandler") BedrockHandler bedrockHandler,
					 @JsonProperty("materialByPoint") ImmutableMap<Point, Material> materialByPoint,
					 @JsonProperty("batches") Set<BedrockBatch> batches,
					 @JsonProperty("updatePoints") Set<Point> updatePoints,
					 @JsonProperty("worldName") String worldName) {
			this.bedrockHandler = bedrockHandler;
			this.materialByPoint = materialByPoint;
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
		model = new Model(new BedrockHandler(world), ImmutableMap.of(), new HashSet<>(), new HashSet<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	//this method is (or should be) thread safe
	public BedrockBatch convert(Set<Point> points, BedrockAuthToken authToken) {
		BedrockBatch batch = new BedrockBatch(points, authToken);
		synchronized (model.mutex) {
			addBatch(batch);
		}

		return batch;
	}
	private void addBatch(BedrockBatch batch) {
		model.batches.add(batch);
		model.updatePoints.addAll(batch.getPoints());
	}

	//this method is (or should be) thread safe
	public void revert(BedrockBatch batch) {
		synchronized (model.mutex) {
			removeBatch(batch);
		}
	}
	private void removeBatch(BedrockBatch batch) {
		model.batches.remove(batch);
		model.updatePoints.addAll(batch.getPoints());
	}

	public void revert(BedrockAuthToken authToken) {
		synchronized (model.mutex) {
			for (BedrockBatch batch : model.batches) {
				if (batch.authorizedBy(authToken)) {
					removeBatch(batch);
				}
			}
		}
	}

	public Set<Point> forceRevertBatchesContaining(Set<Point> forceRevertPoints) {
		Set<Point> forceRevertedPoints = new HashSet<>();

		synchronized (model.mutex) {
			for (BedrockBatch batch : model.batches) {
				Set<Point> batchPoints = batch.getPoints();
				boolean forceRevert = !Collections.disjoint(batchPoints, forceRevertPoints);

				if (forceRevert) {
					forceRevertedPoints.addAll(batchPoints);
					removeBatch(batch);
				}
			}
		}

		return forceRevertedPoints;
	}

	public void onTick() {
		if (model.updatePoints.size() > 0) {
			update();
			model.updatePoints.clear();
		}
	}

	public Material getMaterialOrNull(Point p) {
		return model.materialByPoint.get(p);
	}

	public Map<Point, Material> getMaterialByPointMap() {
		return model.materialByPoint;
	}

	// utils //

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

		//convert/revert updatedPoints
		for (Point p : model.updatePoints) {
			boolean shouldBeConv = shouldBeConverted.contains(p);
			boolean isConv = model.bedrockHandler.isConverted(p);

			//if (isTallDoor) update shouldBeConv (since it also depends on other half of door)
			Material mat = p.getType(model.world);
			boolean isTallDoor = Blocks.isTallDoor(mat);
			if (isTallDoor) {
				Point pOtherHalf = getOtherHalfOfDoor(p);
				if (pOtherHalf != null) {
					boolean otherHalfShouldBeConv = allBatchPoints.contains(pOtherHalf);
					shouldBeConv = shouldBeConv || otherHalfShouldBeConv;
				}
			}

			if (!isConv && shouldBeConv) {
				model.bedrockHandler.convert(p);
			} else if (isConv && !shouldBeConv) {
				model.bedrockHandler.revert(p);
			}
		}

		model.materialByPoint = ImmutableMap.copyOf(model.bedrockHandler.getMaterialByPointMap());
	}

	private Point getOtherHalfOfDoor(Point p) {
		Point pOtherHalf = null;

		Pair<Point, Point> doorTopBottom = model.bedrockHandler.getDoorTopBottom(p);
		if (doorTopBottom != null) {
			Point top = doorTopBottom.getKey();
			Point bottom = doorTopBottom.getValue();

			pOtherHalf = (p.equals(top))?bottom:top;
		}

		return pOtherHalf;
	}
}



//TimedBedrockManager::convertTimed(Set<Point> points)

//TODO: try to refactor to use batch converts
//	BedrockBatch batch = BedrockManager.convert(points);
//	batch.revert();