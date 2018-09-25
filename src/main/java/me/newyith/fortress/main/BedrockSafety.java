package me.newyith.fortress.main;

import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BedrockSafety {
	protected static final transient Object mutex = new Object();

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
		synchronized (mutex) { //really shouldn't need to synchronize here but do it anyway just in case
			this.model = model;
		}
	}

	public BedrockSafety() {
		synchronized (mutex) { //really shouldn't need to synchronize here but do it anyway just in case
			model = new Model(new HashMap<>());
		}
	}

	//-----------------------------------------------------------------------

	public static void safetySync() {
		getInstance().doSafetySync();
	}
	private void doSafetySync() {
		synchronized (mutex) {
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

				ImmutableSet<Point> claimedWallPoints = rune.getGeneratorCore().getClaimedWallPoints();
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
	}

	public static CompletableFuture<Void> record(World world, Set<Point> wallPoints) {
		return getInstance().doRecord(world, wallPoints);
	}
	private CompletableFuture<Void> doRecord(World world, Set<Point> wallPoints) {
		synchronized (mutex) {
//			Debug.start("doRecord() before saveBedrockSafety()");
			String worldName = world.getName();
			if (!model.materialMapByWorld.containsKey(worldName)) {
				model.materialMapByWorld.put(worldName, new HashMap<>());
			}
			final Map<Point, Material> matMapForWorld = model.materialMapByWorld.get(worldName);
			final Map<Point, Material> pretendMatByPoint = BedrockManager.forWorld(world).getOrBuildMaterialByPointMap();

			wallPoints.forEach(p -> {
				Material mat = pretendMatByPoint.get(p);
				if (mat == null) mat = p.getType(world);
				matMapForWorld.put(p, mat);

				//LATER: delete debug if statement later?
				if (mat == Material.BEDROCK) {
					String ANSI_RESET = "\u001B[0m";
					String ANSI_RED = "\u001B[31m";
					Debug.msg(ANSI_RED + "WARNING: BedrockSafety recorded bedrock as original material at " + p + ANSI_RESET);
				}
			});
//			Debug.end("doRecord() before saveBedrockSafety()");
		}

		return SaveLoadManager.saveBedrockSafety();
	}
}



//ensure there will be no abandoned bedrock by keeping a map of materialByPoint for all wallPoints
//	and then onLoad revert any bedrock in materialByPoint that is not supposed to be bedrock
//		supposed to be bedrock if BedrockManager has data for point or if it's an altered point
//Note: save to bedrockSafety.json onGenerate
//Note: keep periodic save and saveWithWorld
