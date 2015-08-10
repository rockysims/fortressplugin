package me.newyith.generator;

import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

public class GeneratorCoreAnimator implements Memorable {
	//saved
	private HashMap<Point, Material> alteredPoints = new HashMap<>();
	private Set<Point> protectedPoints = new HashSet<>();
	private List<List<Point>> generatedLayers = new ArrayList<>();
	private List<List<Point>> animationLayers = new ArrayList<>();
	public boolean animate = true;
	public boolean isChangingGenerated = false;
	public boolean isGeneratingWall = false;

	//not saved
	private long lastFrameTimestamp = 0;
	private final long msPerFrame = 150;

	//------------------------------------------------------------------------------------------------------------------

	public void saveTo(Memory m) {
		Debug.msg("saving alteredPoints: " + alteredPoints.size());
		m.save("alteredPoints", alteredPoints);
		Debug.msg("saved alteredPoints: " + alteredPoints.size());

		m.save("protectedPoints", protectedPoints);
		Debug.msg("saved protectedPoints: " + protectedPoints.size());

		m.save("generatedLayers", generatedLayers);
		Debug.msg("saved generatedLayers: " + generatedLayers.size());

		m.save("animationLayers", animationLayers);
		Debug.msg("saved animationLayers: " + animationLayers.size());

		m.save("animate", animate);
		Debug.msg("saved animate: " + animate);

		m.save("isChangingGenerated", isChangingGenerated);
		Debug.msg("saved isChangingGenerated: " + isChangingGenerated);

		m.save("isGeneratingWall", isGeneratingWall);
		Debug.msg("saved isGeneratingWall: " + isGeneratingWall);

	}

	public static GeneratorCoreAnimator loadFrom(Memory m) {
		HashMap<Point, Material> alteredPoints = m.loadPointMaterialMap("alteredPoints");
		Debug.msg("loaded alteredPoints: " + alteredPoints.size());

		Set<Point> protectedPoints = m.loadPointSet("protectedPoints");
		Debug.msg("loaded protectedPoints: " + protectedPoints.size());

		List<List<Point>> generatedLayers = m.loadLayers("generatedLayers");
		Debug.msg("loaded generatedLayers: " + generatedLayers.size());

		List<List<Point>> animationLayers = m.loadLayers("animationLayers");
		Debug.msg("loaded animationLayers: " + animationLayers.size());

		boolean animate = m.loadBoolean("animate");
		Debug.msg("loaded animate: " + animate);

		boolean isChangingGenerated = m.loadBoolean("isChangingGenerated");
		Debug.msg("loaded isChangingGenerated: " + isChangingGenerated);

		boolean isGeneratingWall = m.loadBoolean("isGeneratingWall");
		Debug.msg("loaded isGeneratingWall: " + isGeneratingWall);


		GeneratorCoreAnimator instance = new GeneratorCoreAnimator(alteredPoints, protectedPoints, generatedLayers, animationLayers, animate, isChangingGenerated, isGeneratingWall);
		return instance;
	}

	private GeneratorCoreAnimator(
			HashMap<Point, Material> alteredPoints,
			Set<Point> protectedPoints,
			List<List<Point>> generatedLayers,
			List<List<Point>> animationLayers,
			boolean animate,
			boolean isChangingGenerated,
			boolean isGeneratingWall) {
		this.alteredPoints = alteredPoints;
		this.protectedPoints = protectedPoints;
		this.generatedLayers = generatedLayers;
		this.animationLayers = animationLayers;
		this.animate = animate;
		this.isChangingGenerated = isChangingGenerated;
		this.isGeneratingWall = isGeneratingWall;
	}

	//------------------------------------------------------------------------------------------------------------------

	public GeneratorCoreAnimator() { }

	public List<List<Point>> getGeneratedLayers() {
		return this.generatedLayers;
	}

	public void generate(List<List<Point>> layers) {
		this.animationLayers = layers;
		isGeneratingWall = true;
		isChangingGenerated = true;
	}

	public void degenerate(boolean animate) {
		animationLayers = generatedLayers;
		isGeneratingWall = false;
		isChangingGenerated = true;

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
		isChangingGenerated = true;
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

		isGeneratingWall = generatedLayers.size() > 0;
	}

	public void tick() {
		if (this.isChangingGenerated) {
			long now = (new Date()).getTime();
			//if (ready to update to next frame)
			if (!this.animate  || now - this.lastFrameTimestamp >= this.msPerFrame ) {
				this.lastFrameTimestamp  = now;

				//update to next frame
				boolean noNextFrame = !this.updateToNextFrame();
				if (noNextFrame) {
					this.isChangingGenerated = false;
				}

				//if (not animating) we finished all at once
				if (!this.animate) {
					this.isChangingGenerated = false;
				}
			}
		}
	}

	// --------- Internal Methods ---------

	private boolean updateToNextFrame() {
		boolean foundLayerToUpdate = false;

		for (int i = 0; i < this.animationLayers.size(); i++) {
			int layerIndex = i;
			//if (degenerating) start from the outer most layer
			if (!this.isGeneratingWall) {
				layerIndex = (animationLayers.size()-1) - i;
			}

			List<Point> layer = new ArrayList<>(this.animationLayers.get(layerIndex)); //make copy to avoid concurrent modification errors (recheck this is needed)

			//try to update layer
			foundLayerToUpdate = updateLayer(layer, layerIndex);
			if (foundLayerToUpdate && this.animate) {
				//updated a layer so we're done with this frame
				break;
			}
		}

		return foundLayerToUpdate;
	}

	private boolean updateLayer(List<Point> layer, int layerIndex) {
		boolean updatedLayer = false;

		for (Point p : layer) {
			if (this.isGeneratingWall) {
				//try to generate block at p
				boolean pGenerated = alter(p) || protect(p);
				updatedLayer = updatedLayer || pGenerated;

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
				updatedLayer = updatedLayer || pDegenerated;

				if (pDegenerated) {
					//remove p from generatedLayers
					if (layerIndex < this.generatedLayers.size()) {
						this.generatedLayers.get(layerIndex).remove(p);
					} //else we would be degenerating another generators wall
				}
			}
		} // end for (Point p : layer)

		return updatedLayer;
	}

	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock();
		if (Wall.isAlterableWallMaterial(b.getType())) {
			this.alteredPoints.put(p, b.getType());
			b.setType(Material.BEDROCK);
			altered = true;
		}

		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (this.alteredPoints.containsKey(p)) {
			Material material = this.alteredPoints.remove(p);
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
		if (!this.protectedPoints.contains(p) && Wall.isProtectableWallMaterial(b.getType())) {
			this.protectedPoints.add(p);
			//TODO: make FortressGeneratorParticlesManager show particles on protectedPoints
			//TODO: make block at p unbreakable
			pointProtected = true;
		}

		return pointProtected;
	}

	private boolean unprotect(Point p) {
		boolean unprotected = false;

		if (this.protectedPoints.contains(p)) {
			this.protectedPoints.remove(p);
			//TODO: make block at p breakable again
			unprotected = true;
		}

		return unprotected;
	}
}
