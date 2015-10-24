package me.newyith.fortresstemp.generator.core;

/*

GenerationTask extends Observable
Core
GeneratorCore extends Core
//PistonCore extends Core
WallMaterials
Rune
RunePattern
Doors (utils for white list doors)

RunesManager
ParticlesManager
 */

import me.newyith.fortresstemp.generator.generation.GenerationTask;
import me.newyith.fortresstemp.generator.generation.WallMaterials;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Wall;
import org.bukkit.Material;

import java.util.*;

public abstract class Core {
	//TODO: save
	//TODO: move saved fields to CoreModel and add 'CoreModel model;' field
	//	model should contain all permanent state
	protected Point anchor;
	private Set<Point> originLayer;
	private HashMap<Point, Material> alteredPoints = new HashMap<>();
	private HashMap<Point, Material> protectedPoints = new HashMap<>();
	private List<Set<Point>> generatedLayers = new ArrayList<>();
	private Set<Point> claimedWallPoints = new HashSet<>();

	//not saved
	private GenerationTask generationTask = null;

	public Core(Point anchor, Set<Point> originLayer) {
		this.anchor = anchor;
		this.originLayer = originLayer;
	}

	protected abstract WallMaterials getWallMats();
	protected abstract void sendMessage(String msg);

	private List<Set<Point>> getGeneratableWallLayers() {
		Set<Point> claimedPoints = getClaimedPointsOfNearbyGenerators();

		//return all connected wall points ignoring (and not traversing) claimedPoints (generationRangeLimit search range)
		List<Set<Point>> allowedWallLayers = getPointsConnectedAsLayers(getWallMats().getWallMaterials(), getWallMats().getGeneratableWallMaterials(), generationRangeLimit, claimedPoints);

		return allowedWallLayers;
	}

	public void generate() {
		//cancel existing generationTask (if any)
		if (generationTask != null) generationTask.cancel();

		//
		List<Set<Point>> generatableLayers = getGeneratableWallLayers();
		List<Set<Point>> wallLayers = Wall.merge(generatableLayers, generatedLayers);
		Set<Point> wallPoints = Wall.flattenLayers(wallLayers);
		updateClaimedPoints(wallPoints);
		updateInsideOutside(wallPoints);








		animator.generate(generatableLayers);







		generationTask = new GenerationTask(null, true);
		generationTask.onAltered((p) -> {
		});


		generationTask.addObserver((Observable generationTask, Object arg) -> {
			out.println("\nReceived response: " + arg);
		});
		generationTask.runTaskAsynchronously();

	}





}
