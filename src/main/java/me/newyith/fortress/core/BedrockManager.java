package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.core.util.BlockRevertData;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

public class BedrockManager {
	private static BedrockManager instance = null;
	public static BedrockManager getInstance() {
		if (instance == null) {
			instance = new BedrockManager();
		}
		return instance;
	}
	public static void setInstance(BedrockManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private final Map<String, Map<Point, BlockRevertData>> revertDataMapByWorld;

		@JsonCreator
		public Model(@JsonProperty("revertDataMapByWorld") Map<String, Map<Point, BlockRevertData>> revertDataMapByWorld) {
			this.revertDataMapByWorld = revertDataMapByWorld;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManager() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static boolean convert(World world, Point p) {
		boolean isConverted = false;

		Map<Point, BlockRevertData> revertData = getDataForWorld(world);
		if (revertData.containsKey(p)) {
			isConverted = true;
		} else {
			Material mat = p.getBlock(world).getType();
			if (Blocks.isTallDoor(mat)) {
				isConverted = convertTallDoor(world, p);
			} else {
				revertData.put(p, new BlockRevertData(world, p));
				p.getBlock(world).setType(Material.BEDROCK);
				isConverted = true;
			}
		}

		return isConverted;
	}

	public static boolean revert(World world, Point p) {
		boolean isReverted = false;

		Map<Point, BlockRevertData> revertData = getDataForWorld(world);
		BlockRevertData revertDatum = revertData.get(p);
		//isBedrock condition is to prevent any chance of duplicating blocks if plugin save gets out of sync with world save
		if (revertDatum != null && p.is(Material.BEDROCK, world)) {
			Material mat = revertDatum.getMaterial();
			if (Blocks.isTallDoor(mat)) {
				isReverted = revertTallDoor(world, p);
			} else {
				revertDatum.revert(world, p);
				isReverted = true;
			}
		} else {
			isReverted = true;
		}
		revertData.remove(p);

		return isReverted;
	}

	public static void forget(World world, Point p) {
		Map<Point, BlockRevertData> revertDataMap = instance.model.revertDataMapByWorld.get(world.getName());
		if (revertDataMap != null) {
			revertDataMap.remove(p);
		}
	}

	private static boolean convertTallDoor(World world, Point p) {
		boolean converted = false;

		//assumes p is a door block (2 block tall doors)
		Map<Point, BlockRevertData> revertData = getDataForWorld(world);
		Pair<Point, Point> doorTopBottom = getDoorTopBottom(world, p);
		if (doorTopBottom != null) {
			Point top = doorTopBottom.getKey();
			Point bottom = doorTopBottom.getValue();

			revertData.put(top, new BlockRevertData(world, top));
			revertData.put(bottom, new BlockRevertData(world, bottom));

			bottom.setType(Material.BEDROCK, world);
			top.setType(Material.BEDROCK, world);

			converted = true;
		}

		return converted;
	}

	private static boolean revertTallDoor(World world, Point p) {
		boolean reverted = false;

		//assumes p is a door block (2 block tall doors)
		Map<Point, BlockRevertData> revertData = getDataForWorld(world);
		Pair<Point, Point> doorTopBottom = getDoorTopBottom(world, p);
		if (doorTopBottom != null) {
			Point top = doorTopBottom.getKey();
			Point bottom = doorTopBottom.getValue();
			BlockRevertData topData = revertData.get(top);
			BlockRevertData bottomData = revertData.get(bottom);

			if (topData != null && bottomData != null) {
				bottomData.revert(world, bottom);
				topData.revert(world, top);

				revertData.remove(top);
				revertData.remove(bottom);

				reverted = true;
			} else {
				Debug.error("BedrockManager::revertTallDoor() failed to find revert data for door's top and/or bottom.");
			}
		}

		return reverted;
	}

	private static Pair<Point, Point> getDoorTopBottom(World world, Point p) {
		Map<Point, BlockRevertData> revertData = getDataForWorld(world);

		//assumes p is a door block
		Point top = null;
		Point bottom = null;
		Point a = p.add(0, 1, 0);
		Point b = p.add(0, -1, 0);
		Material above = a.getType(world);
		Material below = b.getType(world);
		Material middle = p.getType(world);

		if (revertData.containsKey(a)) above = revertData.get(a).getMaterial();
		if (revertData.containsKey(b)) below = revertData.get(b).getMaterial();
		if (revertData.containsKey(p)) middle = revertData.get(p).getMaterial();

		if (above == middle) {
			top = a;
			bottom = p;
		} else if (below == middle) {
			top = p;
			bottom = b;
		}

		if (top == null) {
			Debug.error("getDoorTopBottom() failed.");
			return null;
		} else {
			return new Pair<>(top, bottom);
		}
	}

	public static Material getMaterial(World world, Point p) {
		Material mat = null;

		Map<Point, BlockRevertData> dataForWorld = getDataForWorld(world);
		if (dataForWorld != null) {
			BlockRevertData revertData = dataForWorld.get(p);
			if (revertData != null) {
				mat = revertData.getMaterial();
			}
		}

		return mat;
	}

	public static Map<Point, Material> getMaterialByPointMapForWorld(World world) {
		Map<Point, Material> map = new HashMap<>();

		Map<Point, BlockRevertData> dataForWorld = getDataForWorld(world);
		for (Point p : dataForWorld.keySet()) {
			BlockRevertData revertData = dataForWorld.get(p);
			Material mat = revertData.getMaterial();
			map.put(p, mat);
		}

		return map;
	}

	private static Map<Point, BlockRevertData> getDataForWorld(World world) {
		Map<String, Map<Point, BlockRevertData>> revertDataMapByWorld = instance.model.revertDataMapByWorld;
		Map<Point, BlockRevertData> data = revertDataMapByWorld.get(world.getName());
		if (data == null) {
			data = new HashMap<>();
			revertDataMapByWorld.put(world.getName(), data);
		}
		return data;
	}
}
