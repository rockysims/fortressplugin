package me.newyith.fortressOrig.core;

import me.newyith.fortressOrig.bedrock.BedrockBatch;
import me.newyith.fortressOrig.event.TickTimer;
import me.newyith.fortressOrig.main.FortressesManager;
import me.newyith.fortressOrig.util.Debug;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/*
altered:
	blocks changed to bedrock
protected:
	blocks made unbreakable
generated:
	blocks made unbreakable and blocks changed to bedrock
claimed:
	points the generate thinks it owns
//*/

public class CoreAnimatorOld {
	private static class Model {
		private Point anchorPoint = null;
		//TODO: think about changing alteredBatchesByLayerIndex to alteredBatchByLayerIndex (single batch per layer)
		private Map<Integer, Set<BedrockBatch>> alteredBatchesByLayerIndex = null;
		private Set<Point> protectedPoints = null;
		private List<Set<Point>> generatedLayers = null;
		private List<Set<Point>> animationLayers = null;
		private CoreMaterials coreMats = null;
		private boolean skipAnimation = false;
		private boolean animationInProgress = false;
		private boolean isGeneratingWall = false;
		private String worldName = null;
		private transient World world = null;
		private final transient int ticksPerFrame;
		private transient int animationWaitTicks = 0;
		private transient int curIndex = 0;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("alteredBatchesByLayerIndex") Map<Integer, Set<BedrockBatch>> alteredBatchesByLayerIndex,
					 @JsonProperty("protectedPoints") Set<Point> protectedPoints,
					 @JsonProperty("generatedLayers") List<Set<Point>> generatedLayers,
					 @JsonProperty("animationLayers") List<Set<Point>> animationLayers,
					 @JsonProperty("coreMats") CoreMaterials coreMats,
					 @JsonProperty("skipAnimation") boolean skipAnimation,
					 @JsonProperty("animationInProgress") boolean animationInProgress,
					 @JsonProperty("isGeneratingWall") boolean isGeneratingWall,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.alteredBatchesByLayerIndex = alteredBatchesByLayerIndex;
			this.protectedPoints = protectedPoints;
			this.generatedLayers = generatedLayers;
			this.animationLayers = animationLayers;
			this.coreMats = coreMats;
			this.skipAnimation = skipAnimation;
			this.animationInProgress = animationInProgress;
			this.isGeneratingWall = isGeneratingWall;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.ticksPerFrame = (150 / TickTimer.msPerTick); // msPerFrame / msPerTick
			this.animationWaitTicks = 0;
			this.curIndex = 0;
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreAnimatorOld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public CoreAnimatorOld(World world, Point anchorPoint, CoreMaterials coreMats) {
		Map<Integer, Set<BedrockBatch>> alteredBatchesByLayerIndex = new HashMap<>();
		Set<Point> protectedPoints = new HashSet<>();
		List<Set<Point>> generatedLayers = new ArrayList<>();
		List<Set<Point>> animationLayers = new ArrayList<>();
		boolean skipAnimation = false;
		boolean animationInProgress = false;
		boolean isGeneratingWall = false;
		String worldName = world.getName();
		model = new Model(anchorPoint, alteredBatchesByLayerIndex, protectedPoints, generatedLayers, animationLayers,
				coreMats, skipAnimation, animationInProgress, isGeneratingWall, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public List<Set<Point>> getGeneratedLayers() {
		return model.generatedLayers;
	}

	public void generate(List<Set<Point>> layers) {
		model.animationLayers = layers;
		model.curIndex = 0;
		model.isGeneratingWall = true;
		model.animationInProgress = true;
	}

	public void degenerate(boolean skipAnimation) {
		model.curIndex = 0; //starting from end if degenerating is handled elsewhere
		model.isGeneratingWall = false;
		model.animationInProgress = true;

		if (skipAnimation) {
			model.skipAnimation = true;
			tick();
			model.skipAnimation = false;
		}
	}

	public CoreMaterials getCoreMats() {
		return model.coreMats;
	}

	public Set<Point> getAlteredPoints() {
		//TODO: add model.alteredPoints and keep it updated (so we don't need to recalculate here all the time)
		//	maybe test first to make sure its worth improving performance here?
		Debug.start("CoreAnimator::getAlteredPoints()");
		Set<Point> alteredPoints = new HashSet<>();
		for (Set<BedrockBatch> alteredBatches : model.alteredBatchesByLayerIndex.values()) {
			for (BedrockBatch batch : alteredBatches) {
				alteredPoints.addAll(batch.getPoints());
			}
		}
		Debug.end("CoreAnimator::getAlteredPoints()");

		return alteredPoints;
	}

	public Set<Point> getProtectedPoints() {
		return model.protectedPoints;
	}

	public Set<Point> getGeneratedPoints() {
		Set<Point> generatedPoints = new HashSet<>();
		generatedPoints.addAll(getAlteredPoints());
		generatedPoints.addAll(model.protectedPoints);

		return generatedPoints;
	}

	public Set<Material> getInvalidWallMaterials() {
		return model.coreMats.getInvalidWallMaterials();
	}

	public void tick() {
		if (model.animationInProgress) {
			model.animationWaitTicks++;
			if (model.animationWaitTicks >= model.ticksPerFrame) {
				model.animationWaitTicks = 0;

				while (true) {
					//try to update to next frame
					boolean updatedFrame = updateToNextFrame();
					if (!updatedFrame) {
						//no more layers to update so stop animating
						model.animationInProgress = false;
						break;
					}
					if (updatedFrame && !model.skipAnimation) {
						//updated a layer so we're done with this frame
						break;
					}
				}
			}
		}
	}

	// --------- Internal Methods ---------

	private void onGeneratedChanged() {
		BaseCore core = FortressesManager.forWorld(model.world).getCore(model.anchorPoint);
		if (core != null) {
			core.onGeneratedChanged();
		} else {
			Debug.error("CoreAnimator.onGeneratedChanged(): Core at " + model.anchorPoint + " is null.");
		}
	}

	private boolean updateToNextFrame() {
		boolean updatedToNextFrame = false;

		while (!updatedToNextFrame && model.curIndex < model.animationLayers.size()) {
			int layerIndex = model.curIndex;
			//if (degenerating) start from the outer most layer
			if (!model.isGeneratingWall) {
				layerIndex = (model.animationLayers.size()-1) - model.curIndex;
			}

			//try to update layer
			int updatedCount = updateLayer(layerIndex);
			if (updatedCount > 0) {
				updatedToNextFrame = true;
				onGeneratedChanged(); //particles update
			}

			model.curIndex++;
		}

		return updatedToNextFrame;
	}













	//*/
	private int updateLayer(int layerIndex) {
		return 0;
	}
	/*/ //commented out to allow compile despite errors here

	private int updateLayer(int layerIndex) {
		Set<Point> updatedPoints = new HashSet<>();

		if (model.isGeneratingWall) {
			Set<Point> genPoints = generateLayer(layerIndex);
			updatedPoints.addAll(genPoints);

			//update model.generatedLayers
			if (genPoints.size() > 0) {
				//ensure generatedLayers.get(layerIndex) exists and add genPoints
				while (layerIndex >= model.generatedLayers.size()) {
					model.generatedLayers.add(new HashSet<>());
				}
				model.generatedLayers.get(layerIndex).addAll(genPoints);
			}
		} else {
			Set<Point> degenPoints = degenerateLayer(layerIndex);
			updatedPoints.addAll(degenPoints);

			//update model.generatedLayers
			if (degenPoints.size() > 0) {
				//from generatedLayers.get(layerIndex) remove degenPoints
				if (layerIndex < model.generatedLayers.size()) {
					model.generatedLayers.get(layerIndex).removeAll(degenPoints);
				}
			}
		}

		//TODO: delete commented out block
//		Set<Point> layer = new HashSet<>(model.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors
//		for (Point p : layer) {
//			if (model.isGeneratingWall) {
//				//try to generate block at p
//				boolean pGenerated = alter(p) || protect(p);
//				if (pGenerated) {
//					updatedPoints.add(p);
//
//					//add p to generatedLayers
//					while (layerIndex >= model.generatedLayers.size()) {
//						model.generatedLayers.add(new HashSet<>());
//					}
//					model.generatedLayers.get(layerIndex).add(p);
//				}
//			} else {
//				//try to degenerate block at p
//				boolean pDegenerated = unalter(p) || unprotect(p);
//				if (pDegenerated) {
//					updatedPoints.add(p);
//
//					//remove p from generatedLayers
//					if (layerIndex < model.generatedLayers.size()) {
//						model.generatedLayers.get(layerIndex).remove(p);
//					} //else we would be degenerating another generators wall
//				}
//			}
//
//			if (updatedPoints.size() >= model.maxBlocksPerFrame) {
//				break;
//			}
//		} // end for (Point p : layer)

		if (!model.skipAnimation) {
			//show bedrock wave
			int ms = 4 * model.ticksPerFrame * TickTimer.msPerTick;
			TimedBedrockManager.forWorld(model.world).convert(model.bedrockGroupId, updatedPoints, ms);
		}

		return updatedPoints.size();
	}

	private Set<Point> generateLayer(int layerIndex) {
		Set<Point> layer = new HashSet<>(model.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors
		Set<Point> generatedPoints = new HashSet<>();

		//fill alterPoints and protectPoints from layer
		Set<Point> alterPoints = new HashSet<>();
		Set<Point> protectPoints = new HashSet<>();
		for (Point p : layer) {
			if (!isProtected(p) && isProtectable(p)) {
				protectPoints.add(p);
			} else if (!isAltered(p) && isAlterable(p)) {
				alterPoints.add(p);
			}
		}

		generatedPoints.addAll(alter(alterPoints, layerIndex));
		generatedPoints.addAll(protect(protectPoints));

		return generatedPoints;
	}

	private Set<Point> degenerateLayer(int layerIndex) {
		Set<Point> layer = new HashSet<>(model.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors
		Set<Point> degeneratedPoints = new HashSet<>();

		//fill alterPoints and protectPoints from layer
		Set<Point> alterPoints = new HashSet<>();
		Set<Point> protectPoints = new HashSet<>();
		for (Point p : layer) {
			if (isProtected(p)) {
				protectPoints.add(p);
			} else if (isAltered(p)) {
				alterPoints.add(p);
			}
		}

		Set<BedrockBatch> batches = getAlteredBatches(layerIndex);
		degeneratedPoints.addAll(unalter(alterPoints, layerIndex));
		degeneratedPoints.addAll(unprotect(protectPoints));

		//unprotect protectPoints
		if (protectPoints.size() > 0) {
			removeProtectedPoints(protectPoints);
			degeneratedPoints.addAll(protectPoints);
		}

		//unalter alterPoints
		if (alterPoints.size() > 0) {
			Set<BedrockBatch> batches = getAlteredBatches(layerIndex);
			for (BedrockBatch batch : batches) {
				BedrockManager.forWorld(model.world).revert(batch);
				removeAlteredBatch(batch);
				degeneratedPoints.addAll(batch.getPoints());
			}
		}

		return degeneratedPoints;
	}

	private Set<Point> unalter(Set<Point> alterPoints, int layerIndex) {
		Set<Point> unalteredPoints = new HashSet<>();

		if (alterPoints.size() > 0) {
			Set<BedrockBatch> batches = getAlteredBatches(layerIndex);
			for (BedrockBatch batch : batches) {
				BedrockManager.forWorld(model.world).revert(batch);
				removeAlteredBatch(batch);
				degeneratedPoints.addAll(batch.getPoints());
			}
		}

		return unalteredPoints;
	}


	private Set<Point> alter(Set<Point> alterPoints, int layerIndex) {
		Set<Point> alteredPoints = new HashSet<>();

		if (alterPoints.size() > 0) {
			BedrockBatch batch = new BedrockBatch(model.bedrockGroupId, alterPoints);
			BedrockManager.forWorld(model.world).convert(batch);
			addAlteredBatch(layerIndex, batch);
			alteredPoints.addAll(batch.getPoints());
		}

		return alteredPoints;
	}

	private Set<Point> protect(Set<Point> protectPoints) {
		Set<Point> protectedPoints = new HashSet<>();

		if (protectPoints.size() > 0) {
			addProtectedPoints(protectPoints);
			protectedPoints.addAll(protectPoints);
		}

		return protectedPoints;
	}

	private boolean isAltered(Point p) {
		for (BedrockBatch batch : model.alteredBatches) {
			return batch.contains(p);
		}
		return false;
	}

	private boolean isProtected(Point p) {
		return model.protectedPoints.contains(p);
	}

	private boolean isAlterable(Point p) {
		Material mat = BedrockManager.forWorld(model.world).getMaterialOrNull(p);
		if (mat == null) mat = p.getType(model.world);
		return model.coreMats.isAlterable(mat);
	}

	private boolean isProtectable(Point p) {
		Material mat = BedrockManager.forWorld(model.world).getMaterialOrNull(p);
		if (mat == null) mat = p.getType(model.world);
		return model.coreMats.isProtectable(mat);
	}



	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock(model.world);
		boolean alterable = false;
		alterable = alterable || model.coreMats.isAlterable(b);
		alterable = alterable || model.coreMats.isAlterable(BedrockManagerOld.getMaterial(model.world, p));
		boolean alreadyAltered = model.alteredPoints.contains(p);
		if (alterable && !alreadyAltered) {
			BedrockManagerOld.convert(model.world, p);
			addAlteredPoint(p);
			altered = true;
		}

		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (model.alteredPoints.contains(p)) {
			BedrockManagerOld.revert(model.world, p);
			removeAlteredPoint(p);
			unaltered = true;
		}

		return unaltered;
	}

	private boolean protect(Point p) {
		boolean pointProtected = false;

		Block b = p.getBlock(model.world);
		boolean protectable = false;
		protectable = protectable || model.coreMats.isProtectable(b);
		protectable = protectable || model.coreMats.isProtectable(BedrockManagerOld.getMaterial(model.world, p));
		if (!model.protectedPoints.contains(p) && protectable) {
			addProtectedPoint(p);
			pointProtected = true;
		}

		return pointProtected;
	}

	private boolean unprotect(Point p) {
		boolean unprotected = false;

		if (model.protectedPoints.contains(p)) {
			removeProtectedPoint(p);
			unprotected = true;
		}

		return unprotected;
	}

	private void addProtectedPoint(Point p) {
		model.protectedPoints.add(p);
		FortressesManager.forWorld(model.world).addProtectedPoint(p, model.anchorPoint);
	}

	private void removeProtectedPoint(Point p) {
		model.protectedPoints.remove(p);
		FortressesManager.forWorld(model.world).removeProtectedPoint(p);
	}

	private void addAlteredBatch(int layerIndex, BedrockBatch batch) {
		model.alteredBatches.add(batch);
		FortressesManager.forWorld(model.world).addAlteredPoints(batch.getPoints(), model.anchorPoint);
	}

	private void removeAlteredBatch(BedrockBatch batch) {
		FortressesManager.forWorld(model.world).removeAlteredPoints(batch.getPoints());
		model.alteredBatches.remove(batch);
	}

	//*/

}
