package me.newyith.fortress.generator.core;

import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.generator.BlockRevertData;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

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

public class CoreAnimator {
	private static class Model {
		private Point anchorPoint = null;
		private HashMap<Point, Material> alteredPoints = null;
		private Set<Point> protectedPoints = null;
		private List<Set<Point>> generatedLayers = null;
		private List<Set<Point>> animationLayers = null;
		private LinkedList<Map<Point, BlockRevertData>> waveLayers = null;
		private CoreMaterials coreMats = null;
		private boolean skipAnimation = false;
		private boolean animationInProgress = false;
		private boolean isGeneratingWall = false;
		private int instantLayersRemaining = 0;
		private String worldName = null;
		private transient World world = null;
		private final transient int maxWaveLayers;
		private final transient int maxBlocksPerFrame;
		private final transient int ticksPerFrame;
		private transient int animationWaitTicks = 0;
		private transient int curIndex = 0;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("alteredPoints") HashMap<Point, Material> alteredPoints,
					 @JsonProperty("protectedPoints") Set<Point> protectedPoints,
					 @JsonProperty("generatedLayers") List<Set<Point>> generatedLayers,
					 @JsonProperty("animationLayers") List<Set<Point>> animationLayers,
					 @JsonProperty("waveLayers") LinkedList<Map<Point, BlockRevertData>> waveLayers,
					 @JsonProperty("coreMats") CoreMaterials coreMats,
					 @JsonProperty("skipAnimation") boolean skipAnimation,
					 @JsonProperty("animationInProgress") boolean animationInProgress,
					 @JsonProperty("isGeneratingWall") boolean isGeneratingWall,
					 @JsonProperty("instantLayersRemaining") int instantLayersRemaining,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.alteredPoints = alteredPoints;
			this.protectedPoints = protectedPoints;
			this.generatedLayers = generatedLayers;
			this.animationLayers = animationLayers;
			this.waveLayers = waveLayers;
			this.coreMats = coreMats;
			this.skipAnimation = skipAnimation;
			this.animationInProgress = animationInProgress;
			this.isGeneratingWall = isGeneratingWall;
			this.instantLayersRemaining = instantLayersRemaining;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.maxWaveLayers = 4;
			this.maxBlocksPerFrame = 500;
			this.ticksPerFrame = 150 / TickTimer.msPerTick; // msPerFrame / msPerTick
			this.animationWaitTicks = 0;
			this.curIndex = 0;
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreAnimator(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public CoreAnimator(World world, Point anchorPoint, CoreMaterials coreMats) {
		HashMap<Point, Material> alteredPoints = new HashMap<>();
		Set<Point> protectedPoints = new HashSet<>();
		List<Set<Point>> generatedLayers = new ArrayList<>();
		List<Set<Point>> animationLayers = new ArrayList<>();
		LinkedList<Map<Point, BlockRevertData>> waveLayers = new LinkedList<>();
		boolean skipAnimation = false;
		boolean animationInProgress = false;
		boolean isGeneratingWall = false;
		int instantLayersRemaining = 0;
		String worldName = world.getName();
		model = new Model(anchorPoint, alteredPoints, protectedPoints, generatedLayers, animationLayers, waveLayers,
				coreMats, skipAnimation, animationInProgress, isGeneratingWall, instantLayersRemaining, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public List<Set<Point>> getGeneratedLayers() {
		return model.generatedLayers;
	}

	public Map<Point, Material> getWaveMaterialMap() {
		Map<Point, Material> map = new HashMap<>();

		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			for (Point p : waveLayer.keySet()) {
				BlockRevertData data = waveLayer.get(p);
				map.put(p, data.getMaterial());
			}
		}

		return map;
	}

	public void generate(List<Set<Point>> layers) {
		model.animationLayers = layers;
		model.curIndex = 0;
		model.isGeneratingWall = true;
		model.animationInProgress = true;
		Collections.reverse(model.waveLayers);
		model.instantLayersRemaining = model.waveLayers.size();
	}

	public void degenerate(boolean skipAnimation) {
		model.curIndex = 0; //starting from end if degenerating is handled elsewhere
		model.isGeneratingWall = false;
		model.animationInProgress = true;
		Collections.reverse(model.waveLayers);
		model.instantLayersRemaining = model.waveLayers.size();

		if (skipAnimation) {
			model.skipAnimation = true;
			tick();
			model.skipAnimation = false;
		}
	}

	public void degenerate(Set<Point> pointsToDegenerate) {
		List<Set<Point>> layersToDegenerate = new ArrayList<>();
		layersToDegenerate.add(pointsToDegenerate);

		List<Set<Point>> origAnimationLayers = model.animationLayers;
		boolean origIsGeneratingWall = model.isGeneratingWall;
		boolean origAnimationInProgress = model.animationInProgress;
		model.animationLayers = layersToDegenerate;
		model.isGeneratingWall = false;
		model.animationInProgress = true;
		model.skipAnimation = true;
		tick();
		model.skipAnimation = false;
		model.animationLayers = origAnimationLayers;
		model.isGeneratingWall = origIsGeneratingWall;
		model.animationInProgress = origAnimationInProgress;

		//TODO: remove commented out block after everything seems to be working again without it
		/////// or maybe not since this method would be called for instant degeneration of disconnected points? not sure
		//	shouldn't be needed since tick() naturally updates generatedLayers and calls onGeneratedChanged()
//		//remove pointsToDegenerate from generatedLayers
//		for (Iterator<Set<Point>> it = model.generatedLayers.iterator(); it.hasNext(); ) {
//			Set<Point> layer = it.next();
//			layer.removeAll(pointsToDegenerate);
//			if (layer.size() == 0) {
//				it.remove();
//			}
//		}
//		onGeneratedChanged();
	}

	public CoreMaterials getCoreMats() {
		return model.coreMats;
	}

	public Set<Point> getAlteredPoints() {
		return model.alteredPoints.keySet();
	}

	public Set<Point> getProtectedPoints() {
		return model.protectedPoints;
	}

	public Set<Point> getGeneratedPoints() {
		Set<Point> generatedPoints = new HashSet<>();
		generatedPoints.addAll(model.protectedPoints);
		generatedPoints.addAll(model.alteredPoints.keySet());
		return generatedPoints;
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
		BaseCore core = FortressesManager.getCore(model.anchorPoint);
		if (core != null) {
			core.onGeneratedChanged();
		} else {
			Debug.error("CoreAnimator.onGeneratedChanged(): Core at " + model.anchorPoint + " is null.");
		}
	}






	private void convertWaveLayer(Set<Point> layerPoints) {
		//consider removing old layer
		if (model.waveLayers.size() + 1 > model.maxWaveLayers) {
			Map<Point, BlockRevertData> oldLayer = model.waveLayers.removeFirst();
			revertWaveLayer(oldLayer);
		}

		//add new layer
		Map<Point, BlockRevertData> newLayerData = new HashMap<>();
		for (Point p : layerPoints) {
			newLayerData.put(p, new BlockRevertData(model.world, p));
			p.getBlock(model.world).setType(Material.QUARTZ_BLOCK); //TODO: change to BEDROCK
		}
		model.waveLayers.add(newLayerData);
	}

	private void revertWaveLayer(Map<Point, BlockRevertData> layer) {
		for (Point p : layer.keySet()) {
			layer.get(p).revert(model.world, p);
		}
	}

	private Material getWaveMaterial(Point p) {
		Material material = null;

		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			BlockRevertData data = waveLayer.get(p);
			if (data != null) {
				material = data.getMaterial();
				break;
			}
		}

		return material;
	}

	private void tryRevertWavePoint(Point p) {
		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			BlockRevertData data = waveLayer.get(p);
			if (data != null) {
				data.revert(model.world, p);
				waveLayer.remove(p);
				break;
			}
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

			if (updatedCount < model.maxBlocksPerFrame) {
				model.curIndex++;
			}

			if (updatedCount > 0 && model.instantLayersRemaining > 0) {
				model.instantLayersRemaining--;
				updatedToNextFrame = false;
				Debug.msg("instant");
			}
		}

		if (!updatedToNextFrame && !model.waveLayers.isEmpty()) {
			revertWaveLayer(model.waveLayers.removeFirst());
			updatedToNextFrame = true;
			Debug.msg("finishing wave");
		}

		return updatedToNextFrame;
	}

	private int updateLayer(int layerIndex) {
		Set<Point> updatedPoints = new HashSet<>();

		Set<Point> layer = new HashSet<>(model.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors (recheck this is needed)
		for (Point p : layer) {
			if (model.isGeneratingWall) {
				//try to generate block at p
				boolean pGenerated = alter(p) || protect(p);
				if (pGenerated) {
					updatedPoints.add(p);

					//add p to generatedLayers
					while (layerIndex >= model.generatedLayers.size()) {
						model.generatedLayers.add(new HashSet<>());
					}
					model.generatedLayers.get(layerIndex).add(p);
				}
			} else {
				//try to degenerate block at p
				boolean pDegenerated = unalter(p) || unprotect(p);
				if (pDegenerated) {
					updatedPoints.add(p);

					//remove p from generatedLayers
					if (layerIndex < model.generatedLayers.size()) {
						model.generatedLayers.get(layerIndex).remove(p);
					} //else we would be degenerating another generators wall
				}
			}

			if (updatedPoints.size() >= model.maxBlocksPerFrame) {
				break;
			}
		} // end for (Point p : layer)

//		Debug.msg("layer " + layerIndex + " blockUpdates: " + updatedPoints.size());

		if (!model.skipAnimation && !updatedPoints.isEmpty()) {
//			Debug.msg("<-> convert layerIndex: " + layerIndex);
			convertWaveLayer(updatedPoints);
		}

		return updatedPoints.size();
	}

	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock(model.world);
		boolean alterable = false;
		alterable = alterable || model.coreMats.isAlterable(b);
		alterable = alterable || model.coreMats.isAlterable(getWaveMaterial(p));
		if (alterable) {
			tryRevertWavePoint(p);
			addAlteredPoint(p, b.getType());
			b.setType(Material.BEDROCK);
			altered = true;
		}

		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (model.alteredPoints.containsKey(p)) {
			tryRevertWavePoint(p);
			Material material = removeAlteredPoint(p);
			if (p.getBlock(model.world).getType() == Material.BEDROCK) {
				p.getBlock(model.world).setType(material);
			}
			unaltered = true;
		}

		return unaltered;
	}

	private boolean protect(Point p) {
		boolean pointProtected = false;

		Block b = p.getBlock(model.world);
		boolean protectable = false;
		protectable = protectable || model.coreMats.isProtectable(b);
		protectable = protectable || model.coreMats.isProtectable(getWaveMaterial(p));
		if (!model.protectedPoints.contains(p) && protectable) {
			tryRevertWavePoint(p);
			addProtectedPoint(p);
			pointProtected = true;
		}

		return pointProtected;
	}

	private boolean unprotect(Point p) {
		boolean unprotected = false;

		if (model.protectedPoints.contains(p)) {
			tryRevertWavePoint(p);
			removeProtectedPoint(p);
			unprotected = true;
		}

		return unprotected;
	}

	private void addProtectedPoint(Point p) {
		model.protectedPoints.add(p);
		FortressesManager.addProtectedPoint(p, model.anchorPoint);
	}

	private void removeProtectedPoint(Point p) {
		model.protectedPoints.remove(p);
		FortressesManager.removeProtectedPoint(p);
	}


	private void addAlteredPoint(Point p, Material m) {
		model.alteredPoints.put(p, m);
		FortressesManager.addAlteredPoint(p);
	}

	private Material removeAlteredPoint(Point p) {
		FortressesManager.removeAlteredPoint(p);
		return model.alteredPoints.remove(p);
	}
}
