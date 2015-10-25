package me.newyith.fortressold.generator;

import me.newyith.fortressold.event.TickTimer;
import me.newyith.fortressold.main.FortressPlugin;
import me.newyith.fortressold.memory.AbstractMemory;
import me.newyith.fortressold.memory.Memorable;
import me.newyith.fortressold.util.Chat;
import me.newyith.fortressold.util.Debug;
import me.newyith.fortressold.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

public class GeneratorCoreAnimator implements Memorable {
	//saved
	private Point anchorPoint = null;
	private HashMap<Point, Material> alteredPoints = new HashMap<>();
	private Set<Point> protectedPoints = new HashSet<>();
	private List<List<Point>> generatedLayers = new ArrayList<>();
	private List<List<Point>> animationLayers = new ArrayList<>();
	public boolean animate = true;
	public boolean animationInProgress = false;
	public boolean isGeneratingWall = false;
	public WallMaterials wallMats;

	//not saved
	private final int maxBlocksPerFrame = 500;
	private final int ticksPerFrame = 150 / TickTimer.msPerTick; // msPerFrame / msPerTick
	private int waitTicks = 0;
	private int curIndex = 0;

	//------------------------------------------------------------------------------------------------------------------

	public void saveTo(AbstractMemory<?> m) {
		m.save("anchorPoint", anchorPoint);
//		Debug.msg("saved anchorPoint: " + anchorPoint);

//		Debug.msg("saving alteredPoints: " + alteredPoints.size());
		m.savePointMaterialMapCompact("alteredPoints", alteredPoints);
//		Debug.msg("saved alteredPoints: " + alteredPoints.size());

		m.savePointSetCompact("protectedPoints", protectedPoints);
//		Debug.msg("saved protectedPoints: " + protectedPoints.size());

		//TODO: rebuild generatedLayers instead of save/load?
		m.saveLayersCompact("generatedLayers", generatedLayers);
//		Debug.msg("saved generatedLayers: " + generatedLayers.size());

		m.saveLayersCompact("animationLayers", animationLayers);
//		Debug.msg("saved animationLayers: " + animationLayers.size());

		m.save("animate", animate);
//		Debug.msg("saved animate: " + animate);

		m.save("animationInProgress", animationInProgress);
//		Debug.msg("saved animationInProgress: " + animationInProgress);

		m.save("isGeneratingWall", isGeneratingWall);
//		Debug.msg("saved isGeneratingWall: " + isGeneratingWall);

	}

	public static GeneratorCoreAnimator loadFrom(AbstractMemory<?> m) {
		Point anchorPoint = m.loadPoint("anchorPoint");
//		Debug.msg("loaded anchorPoint: " + anchorPoint);

		HashMap<Point, Material> alteredPoints = m.loadPointMaterialMapCompact("alteredPoints");
//		Debug.msg("loaded alteredPoints: " + alteredPoints.size());

		Set<Point> protectedPoints = m.loadPointSetCompact("protectedPoints");
//		Debug.msg("loaded protectedPoints: " + protectedPoints.size());

		List<List<Point>> generatedLayers = m.loadLayersCompact("generatedLayers");
//		Debug.msg("loaded generatedLayers: " + generatedLayers.size());

		List<List<Point>> animationLayers = m.loadLayersCompact("animationLayers");
//		Debug.msg("loaded animationLayers: " + animationLayers.size());

		boolean animate = m.loadBoolean("animate");
//		Debug.msg("loaded animate: " + animate);

		boolean animationInProgress = m.loadBoolean("animationInProgress");
//		Debug.msg("loaded animationInProgress: " + animationInProgress);

		boolean isGeneratingWall = m.loadBoolean("isGeneratingWall");
//		Debug.msg("loaded isGeneratingWall: " + isGeneratingWall);

		GeneratorCoreAnimator instance = new GeneratorCoreAnimator(anchorPoint, alteredPoints, protectedPoints, generatedLayers, animationLayers, animate, animationInProgress, isGeneratingWall);
		return instance;
	}

	private GeneratorCoreAnimator(
			Point anchorPoint,
			HashMap<Point, Material> alteredPoints,
			Set<Point> protectedPoints,
			List<List<Point>> generatedLayers,
			List<List<Point>> animationLayers,
			boolean animate,
			boolean animationInProgress,
			boolean isGeneratingWall) {
		this.anchorPoint = anchorPoint;
		this.alteredPoints = alteredPoints;
		this.protectedPoints = protectedPoints;
		this.generatedLayers = generatedLayers;
		this.animationLayers = animationLayers;
		this.animate = animate;
		this.animationInProgress = animationInProgress;
		this.isGeneratingWall = isGeneratingWall;
		this.wallMats = new WallMaterials(anchorPoint);

		//onGeneratedChanged() called by runes manager (second stage loading)
	}

	//------------------------------------------------------------------------------------------------------------------

	public GeneratorCoreAnimator(Point anchorPoint) {
		this.anchorPoint = anchorPoint;
		this.wallMats = new WallMaterials(anchorPoint);
	}

	public List<List<Point>> getGeneratedLayers() {
		return this.generatedLayers;
	}

	public void generate(List<List<Point>> layers) {
		this.animationLayers = layers;
		curIndex = 0;
		isGeneratingWall = true;
		animationInProgress = true;
	}

	public void degenerate(boolean animate) {
		animationLayers = generatedLayers;
		curIndex = 0; //starting from end if degenerating is handled elsewhere
		isGeneratingWall = false;
		animationInProgress = true;

		if (!animate) {
			this.animate = false;
			tick();
			this.animate = true;
		}
	}

	public void degenerate(Set<Point> pointsToDegenerate) {
		List<Point> layerToDegenerate = new ArrayList<>();
		layerToDegenerate.addAll(pointsToDegenerate);

		List<List<Point>> layersToDegenerate = new ArrayList<>();
		layersToDegenerate.add(layerToDegenerate);

		this.animationLayers = layersToDegenerate;
		isGeneratingWall = false;
		animationInProgress = true;
		animate = false;
		tick();
		animate = true;

		//remove pointsToDegenerate from generatedLayers
		for (Iterator<List<Point>> itr = generatedLayers.iterator(); itr.hasNext(); ) {
			List<Point> layer = itr.next();
			layer.removeAll(pointsToDegenerate);
			if (layer.size() == 0) {
				itr.remove();
			}
		}
		onGeneratedChanged();

		isGeneratingWall = generatedLayers.size() > 0;
	}

	public Set<Point> getAlteredPoints() {
		return alteredPoints.keySet();
	}

	public Set<Point> getProtectedPoints() {
		return protectedPoints;
	}

	public Set<Point> getGeneratedPoints() {
		Set<Point> generatedPoints = new HashSet<>();
		generatedPoints.addAll(protectedPoints);
		generatedPoints.addAll(alteredPoints.keySet());
		return generatedPoints;
	}

	public void tick() {
		if (animationInProgress) {
			waitTicks++;
			if (waitTicks >= ticksPerFrame) {
				waitTicks = 0;

				while (true) {
					//try to update to next frame
					boolean updatedFrame = updateToNextFrame();
					if (!updatedFrame) {
						//no more layers to update so stop animating
						animationInProgress = false;
						break;
					}
					if (updatedFrame && this.animate) {
						//updated a layer so we're done with this frame
						break;
					}
				}
			}
		}
	}

	// --------- Internal Methods ---------

	private void onGeneratedChanged() {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		if (rune != null) {
			rune.onGeneratedChanged();
		} //rune can be null during init sometimes?
		else { //TODO: remove the else part (after testing?)
			Debug.msg("NULL RUNE AT " + anchorPoint);
		}
	}

	private boolean updateToNextFrame() {
		boolean updatedToNextFrame = false;

		//check we haven't hit block limit
		int generatedCount = alteredPoints.size() + protectedPoints.size();
		if (!isGeneratingWall || generatedCount < FortressPlugin.config_generatorBlockLimit) {
			while (!updatedToNextFrame && curIndex < animationLayers.size()) {
				int layerIndex = curIndex;
				//if (degenerating) start from the outer most layer
				if (!this.isGeneratingWall) {
					layerIndex = (animationLayers.size()-1) - curIndex;
				}

				List<Point> layer = new ArrayList<>(this.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors (recheck this is needed)

				//try to update layer
				int updatedCount = updateLayer(layer, layerIndex);
				if (updatedCount > 0) {
					updatedToNextFrame = true;
					onGeneratedChanged();
				}

				if (updatedCount < maxBlocksPerFrame) {
					curIndex++;
				}
			}
		} else {
			String msg = "Fortress generator reached limit of " + String.valueOf(FortressPlugin.config_generatorBlockLimit) + " blocks.";
			msg = ChatColor.AQUA + msg;
			Chat.ranged(msg, anchorPoint, 16);
		}

		return updatedToNextFrame;
	}

	private int updateLayer(List<Point> layer, int layerIndex) {
		int blockUpdates = 0;

		for (Point p : layer) {
			if (this.isGeneratingWall) {
				//try to generate block at p
				boolean pGenerated = generate(p);
				if (pGenerated) {
					blockUpdates++;
				}

				if (pGenerated) {
					//add p to generatedLayers
					while (layerIndex >= this.generatedLayers.size()) {
						this.generatedLayers.add(new ArrayList<>());
					}
					this.generatedLayers.get(layerIndex).add(p);
				}
			} else {
				//try to degenerate block at p
				boolean pDegenerated = unalter(p) || unprotect(p);
				if (pDegenerated) {
					blockUpdates++;
				}

				if (pDegenerated) {
					//remove p from generatedLayers
					if (layerIndex < this.generatedLayers.size()) {
						this.generatedLayers.get(layerIndex).remove(p);
					} //else we would be degenerating another generators wall
				}
			}

			if (blockUpdates >= maxBlocksPerFrame) {
				break;
			}
		} // end for (Point p : layer)

//		Debug.msg("layer " + layerIndex + " blockUpdates: " + blockUpdates);

		return blockUpdates;
	}

	private boolean generate(Point p) {
		boolean generated = false;

		int generatedCount = alteredPoints.size() + protectedPoints.size();
		if (generatedCount < FortressPlugin.config_generatorBlockLimit) {
			generated = alter(p) || protect(p);
		}

		return generated;
	}

	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock();
		if (wallMats.isAlterable(b)) {
			addAlteredPoint(p, b.getType());
			b.setType(Material.BEDROCK);
			altered = true;
		}

		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (this.alteredPoints.containsKey(p)) {
			Material material = removeAlteredPoint(p);
			if (p.getBlock().getType() == Material.BEDROCK) {
				p.getBlock().setType(material);
			}
			unaltered = true;
		}

		return unaltered;
	}

	private boolean protect(Point p) {
		boolean pointProtected = false;

		Block b = p.getBlock();
		if (!this.protectedPoints.contains(p) && wallMats.isProtectable(b)) {
			addProtectedPoint(p);
			//TODO: make FortressGeneratorParticlesManager show particles on protectedPoints?
			pointProtected = true;
		}

		return pointProtected;
	}

	private boolean unprotect(Point p) {
		boolean unprotected = false;

		if (this.protectedPoints.contains(p)) {
			removeProtectedPoint(p);
			unprotected = true;
		}

		return unprotected;
	}

	private void addProtectedPoint(Point p) {
		this.protectedPoints.add(p);
		FortressGeneratorRunesManager.addProtectedPoint(p, anchorPoint);
	}

	private void removeProtectedPoint(Point p) {
		this.protectedPoints.remove(p);
		FortressGeneratorRunesManager.removeProtectedPoint(p);
	}


	private void addAlteredPoint(Point p, Material m) {
		this.alteredPoints.put(p, m);
		FortressGeneratorRunesManager.addAlteredPoint(p);
	}

	private Material removeAlteredPoint(Point p) {
		FortressGeneratorRunesManager.removeAlteredPoint(p);
		return this.alteredPoints.remove(p);
	}
}
