package me.newyith.fortress.generator.core;

import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.generator.WallMaterials;
import me.newyith.fortress.generator.rune.GeneratorRune;
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

public class CoreAnimator {
	private static class Model {
		private Point anchorPoint = null;
		private HashMap<Point, Material> alteredPoints = null;
		private Set<Point> protectedPoints = null;
		private List<Set<Point>> generatedLayers = null;
		private List<Set<Point>> animationLayers = null;
		private boolean skipAnimation = false;
		private boolean animationInProgress = false;
		private boolean isGeneratingWall = false;
		private String worldName = null;
		private transient World world = null;
		private transient final int maxBlocksPerFrame;
		private transient final int ticksPerFrame;
		private transient int animationWaitTicks = 0;
		private transient int curIndex = 0;
		public transient WallMaterials wallMats;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("alteredPoints") HashMap<Point, Material> alteredPoints,
					 @JsonProperty("protectedPoints") Set<Point> protectedPoints,
					 @JsonProperty("generatedLayers") List<Set<Point>> generatedLayers,
					 @JsonProperty("animationLayers") List<Set<Point>> animationLayers,
					 @JsonProperty("skipAnimation") boolean skipAnimation,
					 @JsonProperty("animationInProgress") boolean animationInProgress,
					 @JsonProperty("isGeneratingWall") boolean isGeneratingWall,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.alteredPoints = alteredPoints;
			this.protectedPoints = protectedPoints;
			this.generatedLayers = generatedLayers;
			this.animationLayers = animationLayers;
			this.skipAnimation = skipAnimation;
			this.animationInProgress = animationInProgress;
			this.isGeneratingWall = isGeneratingWall;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.maxBlocksPerFrame = 500;
			this.ticksPerFrame = 150 / TickTimer.msPerTick; // msPerFrame / msPerTick
			this.animationWaitTicks = 0;
			this.curIndex = 0;
			this.wallMats = new WallMaterials(anchorPoint);
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreAnimator(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public CoreAnimator(World world, Point anchorPoint) {
		HashMap<Point, Material> alteredPoints = new HashMap<>();
		Set<Point> protectedPoints = new HashSet<>();
		List<Set<Point>> generatedLayers = new ArrayList<>();
		List<Set<Point>> animationLayers = new ArrayList<>();
		boolean skipAnimation = false;
		boolean animationInProgress = false;
		boolean isGeneratingWall = false;
		String worldName = world.getName();
		model = new Model(anchorPoint, alteredPoints, protectedPoints, generatedLayers,
				animationLayers, skipAnimation, animationInProgress, isGeneratingWall, worldName);
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
		model.animationLayers = model.generatedLayers;
		model.curIndex = 0; //starting from end if degenerating is handled elsewhere
		model.isGeneratingWall = false;
		model.animationInProgress = true;

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
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		if (rune != null) {
			rune.getGeneratorCore().onGeneratedChanged();
		} //rune can be null during init sometimes?
		else { //TODO: remove the else part (after testing?)
			Debug.error("CoreAnimator.onGeneratedChanged(): Rune at " + model.anchorPoint + " is null.");
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
				onGeneratedChanged();
			}

			if (updatedCount < model.maxBlocksPerFrame) {
				model.curIndex++;
			}
		}

		return updatedToNextFrame;
	}

	private int updateLayer(int layerIndex) {
		int blockUpdates = 0;

		Set<Point> layer = new HashSet<>(model.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors (recheck this is needed)
		for (Point p : layer) {
			if (model.isGeneratingWall) {
				//try to generate block at p
				boolean pGenerated = alter(p) || protect(p);
				if (pGenerated) {
					blockUpdates++;
				}

				if (pGenerated) {
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
					blockUpdates++;
				}

				if (pDegenerated) {
					//remove p from generatedLayers
					if (layerIndex < model.generatedLayers.size()) {
						model.generatedLayers.get(layerIndex).remove(p);
					} //else we would be degenerating another generators wall
				}
			}

			if (blockUpdates >= model.maxBlocksPerFrame) {
				break;
			}
		} // end for (Point p : layer)

//		Debug.msg("layer " + layerIndex + " blockUpdates: " + blockUpdates);

		return blockUpdates;
	}

	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock(model.world);
		if (model.wallMats.isAlterable(b)) {
			addAlteredPoint(p, b.getType());
			b.setType(Material.BEDROCK);
			altered = true;
		}

		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (model.alteredPoints.containsKey(p)) {
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
		if (!model.protectedPoints.contains(p) && model.wallMats.isProtectable(b)) {
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
