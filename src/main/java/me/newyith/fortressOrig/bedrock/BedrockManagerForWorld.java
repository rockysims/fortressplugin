package me.newyith.fortressOrig.bedrock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import javafx.util.Pair;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Debug;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BedrockManagerForWorld {
	private static class Model {
		private final BedrockHandler bedrockHandler;
		private ImmutableMap<Point, Material> materialByPoint;
		private final Set<ForceReversionBatch> forceReversionBatches;
		private final Set<BedrockBatch> batches;
		private final Set<Point> updatePoints;
		private final String worldName;
		private final transient World world;
		private final transient Object mutex;

		@JsonCreator
		public Model(@JsonProperty("bedrockHandler") BedrockHandler bedrockHandler,
					 @JsonProperty("materialByPoint") Map<Point, Material> materialByPoint,
					 @JsonProperty("forceReversionBatches") Set<ForceReversionBatch> forceReversionBatches,
					 @JsonProperty("batches") Set<BedrockBatch> batches,
					 @JsonProperty("updatePoints") Set<Point> updatePoints,
					 @JsonProperty("worldName") String worldName) {
			this.bedrockHandler = bedrockHandler;
			this.materialByPoint = ImmutableMap.copyOf(materialByPoint);
			this.forceReversionBatches = forceReversionBatches;
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
	public BedrockManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManagerForWorld(World world) {
		model = new Model(new BedrockHandler(world), ImmutableMap.of(), new HashSet<>(), new HashSet<>(), new HashSet<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	//this method is (or should be) thread safe
	public void addForceReversion(ForceReversionBatch batch) {
		synchronized (model.mutex) {
			model.forceReversionBatches.add(batch);
			model.updatePoints.addAll(batch.getPoints());
		}
	}

	//this method is (or should be) thread safe
	public void removeForceReversion(ForceReversionBatch batch) {
		synchronized (model.mutex) {
			model.forceReversionBatches.remove(batch);
			model.updatePoints.addAll(batch.getPoints());
			batch.destroy();
		}
	}

	//---

	//this method is (or should be) thread safe
	public BedrockBatch convert(BedrockAuthToken authToken, Set<Point> points) {
		BedrockBatch batch = new BedrockBatch(authToken, points);
		convert(batch);
		return batch;
	}

	//this method is (or should be) thread safe
	public void convert(BedrockBatch batch) {
		synchronized (model.mutex) {
			addBatch(batch);
		}
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
		batch.destroy();
	}

	public void revert(BedrockAuthToken authToken) {
		synchronized (model.mutex) {
			ImmutableSet.copyOf(model.batches) //copy to avoid concurrent modification exception
					.stream()
					.filter(batch -> batch.authorizedBy(authToken))
					.forEach(this::removeBatch);
		}
	}

	//TODO: consider getting rid of the method if we can (quite possible it's needed)
	public Set<Point> forceRevertBatchesContaining(Set<Point> forceRevertPoints) {
		Set<Point> forceRevertedPoints = new HashSet<>();

		synchronized (model.mutex) {
			ImmutableSet<BedrockBatch> origBatches = ImmutableSet.copyOf(model.batches); //copy to avoid concurrent modification exception
			for (BedrockBatch batch : origBatches) {
				Set<Point> batchPoints = batch.getPoints();
				boolean forceRevert = !Collections.disjoint(batchPoints, forceRevertPoints);

				if (forceRevert) {
					forceRevertedPoints.addAll(batchPoints);
					removeBatch(batch);
				}
			}
		}

		if (forceRevertedPoints.size() > 0) {
			Debug.warn("Force reverted " + forceRevertedPoints.size() + " points.");
		}

		return forceRevertedPoints;
	}

	//---

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
		Set<Point> allForceReversionPoints = new HashSet<>();
		Set<Point> shouldBeConverted;
		synchronized (model.mutex) {
			Debug.start("fillAllBatchPoints");
			for (BedrockBatch batch : model.batches) {
				allBatchPoints.addAll(batch.getPoints());
			}
			for (ForceReversionBatch forceReversionBatch : model.forceReversionBatches) {
				allForceReversionPoints.addAll(forceReversionBatch.getPoints());
			}
			Debug.end("fillAllBatchPoints");

			shouldBeConverted = model.updatePoints.parallelStream()
					.filter(allBatchPoints::contains)
					.filter(p -> !allForceReversionPoints.contains(p))
					.collect(Collectors.toSet());
		}

		//convert/revert updatedPoints
		for (Point p : model.updatePoints) {
			boolean shouldBeConv = shouldBeConverted.contains(p);
			boolean isConv = model.bedrockHandler.isConverted(p);
//			Debug.msg(isConv + "/" + shouldBeConv + " is/should at " + p);

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