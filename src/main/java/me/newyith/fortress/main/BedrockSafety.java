package me.newyith.fortress.main;

import me.newyith.fortress.core.BedrockManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
		Debug.msg("ignoring call to safetySync()");
		//getInstance().doSafetySync(); //TODO: uncomment this out
	}
	public void doSafetySync() {
		Debug.msg("doSafetySync() called");
		//revert any bedrock in materialByPoint that is not supposed to be bedrock
		//	supposed to be bedrock if BedrockManager has data for point or if it's an altered point

		for (String worldName : model.materialMapByWorld.keySet()) {
			Map<Point, Material> materialByPoint = model.materialMapByWorld.remove(worldName);
			World world = Bukkit.getWorld(worldName);
			for (Point p : materialByPoint.keySet()) {
				if (p.is(Material.BEDROCK, world)) {
					boolean isAltered = FortressesManager.isAltered(p);
					boolean isKnown = BedrockManager.getMaterial(world, p) != null;
					boolean safeBedrock = isAltered || isKnown;
					if (!safeBedrock) {
						Material mat = materialByPoint.get(p);
						p.setType(mat, world);
						Debug.msg("set !safeBedrock at " + p + " back to " + mat); //TODO: delete this line
					}
				}
			}
		}
	}

	public static void record(World world, Set<Point> wallPoints) {
		getInstance().doRecord(world, wallPoints);
	}
	public void doRecord(World world, Set<Point> wallPoints) {
		//TODO: make altered points use BedrockManager or change BedrockSafety::doRecord() to also look up real material for altered points
		for (Point p : wallPoints) {
			Material mat = BedrockManager.getMaterial(world, p);
			if (mat == null) mat = p.getType(world);

			String worldName = world.getName();
			if (!model.materialMapByWorld.containsKey(worldName)) {
				model.materialMapByWorld.put(worldName, new HashMap<>());
			}
			model.materialMapByWorld.get(worldName).put(p, mat);
		}

		SaveLoadManager.saveBedrockSafety();
	}
}




//ensure there will be no abandoned bedrock by keeping a map of materialByPoint for all wallPoints
//	and then onLoad revert any bedrock in materialByPoint that is not supposed to be bedrock
//		supposed to be bedrock if BedrockManager has data for point or if it's an altered point
//Note: save to bedrockSafety.json onGenerate
//Note: keep periodic save and saveWithWorld
