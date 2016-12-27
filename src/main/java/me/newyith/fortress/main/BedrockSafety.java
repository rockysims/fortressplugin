package me.newyith.fortress.main;

import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class BedrockSafety {
	private static BedrockSafety instance = null;
	public static BedrockSafety getInstance() {
		if (instance == null) {
			instance = new BedrockSafety();
		}
		return instance;
	}
	public static void setInstance(BedrockSafety newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private final Map<String, Map<Point, Material>> materialMapByWorld;

		@JsonCreator
		public Model(@JsonProperty("materialMapByWorld") Map<String, Map<Point, Material>> materialMapByWorld) {
			this.materialMapByWorld = materialMapByWorld;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockSafety(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockSafety() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static void safetySync() {
		getInstance().doSafetySync();
	}
	public void doSafetySync() {
		//revert any bedrock in materialByPoint that is not supposed to be bedrock
		//	allowed to be bedrock if a generator claims point as claimed wall point

		//Debug.msg("doSafetySync() called");

		//fill claimedWallPointsByWorld
		Map<String, Set<Point>> claimedWallPointsByWorld = new HashMap<>();
		Set<GeneratorRune> runes = FortressesManager.getRunesInAllWorlds();
		for (GeneratorRune rune : runes) {
			String worldName = rune.getPattern().getWorld().getName();
			if (!claimedWallPointsByWorld.containsKey(worldName)) {
				claimedWallPointsByWorld.put(worldName, new HashSet<>());
			}

			Set<Point> claimedWallPoints = rune.getGeneratorCore().getClaimedWallPoints();
			claimedWallPointsByWorld.get(worldName).addAll(claimedWallPoints);
		}

		//revert any unsafe bedrock
		for (String worldName : model.materialMapByWorld.keySet()) {
			Map<Point, Material> materialByPoint = model.materialMapByWorld.get(worldName);
			Set<Point> claimedWallPoints = claimedWallPointsByWorld.get(worldName);
			if (claimedWallPoints == null) {
				claimedWallPoints = new HashSet<>();
			}
			World world = Bukkit.getWorld(worldName);
			Set<Point> unsafeBedrock = new HashSet<>();
			Set<Point> materialByPointKeys = new HashSet<>(materialByPoint.keySet()); //copy to avoid concurrent modification
			for (Point p : materialByPointKeys) {
				if (p.is(Material.BEDROCK, world)) {
					boolean managedBedrock = BedrockManager.forWorld(world).getMaterialOrNull(p) != null;
					boolean safeBedrock = claimedWallPoints.contains(p) && managedBedrock;
					if (!safeBedrock) {
						unsafeBedrock.add(p);
					}
				}
			}

			//give BedrockManager a chance to revert unsafeBedrock (try to handle tall doors gracefully)
			Set<Point> forceRevertedPoints = BedrockManager.forWorld(world).forceRevertBatchesContaining(unsafeBedrock);

			//ensure unsafeBedrock is really reverted
			for (Point p : unsafeBedrock) {
				Material mat = materialByPoint.remove(p);
				p.setType(mat, world);
				Debug.warn("set !safeBedrock at " + p + " back to " + mat + " (force reverted " + (forceRevertedPoints.size() - 1) + " other points)");
			}
		}
		model.materialMapByWorld.clear();
	}

	public static void record(World world, Set<Point> wallPoints) {
		getInstance().doRecord(world, wallPoints);
	}
	public void doRecord(World world, Set<Point> wallPoints) {
		for (Point p : wallPoints) {
			Material mat = BedrockManager.forWorld(world).getMaterialOrNull(p);
			if (mat == null) mat = p.getType(world);

			String worldName = world.getName();
			if (!model.materialMapByWorld.containsKey(worldName)) {
				model.materialMapByWorld.put(worldName, new HashMap<>());
			}
			model.materialMapByWorld.get(worldName).put(p, mat);

			//LATER: delete debug if statement later?
			if (mat == Material.BEDROCK) {
				String ANSI_RESET = "\u001B[0m";
				String ANSI_RED = "\u001B[31m";
				Debug.msg(ANSI_RED + "WARNING: BedrockSafety recorded bedrock as original material at " + p + ANSI_RESET);
			}
		}

		SaveLoadManager.saveBedrockSafety();
	}
}




//ensure there will be no abandoned bedrock by keeping a map of materialByPoint for all wallPoints
//	and then onLoad revert any bedrock in materialByPoint that is not supposed to be bedrock
//		supposed to be bedrock if BedrockManager has data for point or if it's an altered point
//Note: save to bedrockSafety.json onGenerate
//Note: keep periodic save and saveWithWorld
