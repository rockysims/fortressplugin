package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.bedrock.util.ManagedBedrock;
import me.newyith.fortress.bedrock.util.ManagedBedrockBase;
import me.newyith.fortress.bedrock.util.ManagedBedrockDoor;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BedrockManagerOld {
	private static BedrockManagerOld instance = null;
	public static BedrockManagerOld getInstance() {
		if (instance == null) {
			instance = new BedrockManagerOld();
		}
		return instance;
	}
	public static void setInstance(BedrockManagerOld newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private final Map<String, Map<Point, ManagedBedrockBase>> managedBedrockMapByWorld;

		@JsonCreator
		public Model(@JsonProperty("managedBedrockMapByWorld") Map<String, Map<Point, ManagedBedrockBase>> managedBedrockMapByWorld) {
			this.managedBedrockMapByWorld = managedBedrockMapByWorld;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockManagerOld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManagerOld() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static void convert(World world, Point p) {
//		Debug.msg("--");
//		Debug.msg("BedrockManager::convert() " + p);
		ManagedBedrockBase managedBedrock = instance.ensureManagedBedrockAt(world, p);
		managedBedrock.convert(world);
	}

	public static void revert(World world, Point p) {
		revert(world, p, false);
	}

	private static void revert(World world, Point p, boolean fullRevert) {
//		Debug.msg("--");
//		Debug.msg("BedrockManager::revert() " + p);
		ManagedBedrockBase managedBedrock = instance.getManagedBedrock(world, p);
		if (managedBedrock != null) {
			managedBedrock.revert(world);
			if (!managedBedrock.isConverted()) {
				if (managedBedrock instanceof ManagedBedrockDoor) {
					ManagedBedrockDoor managedBedrockDoor = (ManagedBedrockDoor) managedBedrock;
					Point top = managedBedrockDoor.getTop();
					Point bottom = managedBedrockDoor.getBottom(); //null if trap door
					instance.removeManagedBedrock(world, top);
					if (bottom != null) instance.removeManagedBedrock(world, bottom);
				} else {
					instance.removeManagedBedrock(world, p);
				}
			}
		}
	}

	public static void fullRevert(World world, Point p) {
		ManagedBedrockBase managedBedrock = instance.getManagedBedrock(world, p);
		if (managedBedrock != null && managedBedrock.isConverted()) {
			revert(world, p, true); //true means full revert
		}
	}

	public static void forget(World world, Point p) {
		instance.removeManagedBedrock(world, p);
	}

	public static Material getMaterial(World world, Point p) {
		Material mat = null;

		ManagedBedrockBase managedBedrock = instance.getManagedBedrock(world, p);
		if (managedBedrock != null) {
			mat = managedBedrock.getMaterial(p);
		}

		return mat;
	}

	//synchronized because called by getGeneratableWallLayers() which is called from async in getGenPrepDataFuture()
	public static synchronized Map<Point, Material> getMaterialByPointMapForWorld(World world) {
		Map<Point, Material> map = new HashMap<>();

		Map<Point, ManagedBedrockBase> managedBedrockMap = instance.model.managedBedrockMapByWorld.get(world.getName());
		if (managedBedrockMap != null) {
			Random random = new Random();
			int rand = random.nextInt(10000);
			Debug.msg("start getMaterialByPointMapForWorld " + rand);
			for (Point p : managedBedrockMap.keySet()) {
				ManagedBedrockBase managedBedrock = managedBedrockMap.get(p);
				map.put(p, managedBedrock.getMaterial(p));
			}
			Debug.msg("end getMaterialByPointMapForWorld " + rand);
		}

		return map;
	}

	// utils //

	private Pair<Point, Point> getDoorTopBottom(World world, Point p) {
		//assumes p is a door block
		Point top = null;
		Point bottom = null;
		Point a = p.add(0, 1, 0);
		Point b = p.add(0, -1, 0);
		Material above = a.getType(world);
		Material below = b.getType(world);
		Material middle = p.getType(world);

		if (isConverted(world, a)) above = getMaterial(world, a);
		if (isConverted(world, b)) below = getMaterial(world, b);
		if (isConverted(world, p)) middle = getMaterial(world, p);

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
	private boolean isConverted(World world, Point p) {
		ManagedBedrockBase managedBedrock = instance.getManagedBedrock(world, p);
		return managedBedrock != null && managedBedrock.isConverted();
	}

	private synchronized ManagedBedrockBase ensureManagedBedrockAt(World world, Point p) {
//		Random random = new Random();
//		int rand = random.nextInt(10000);
//		Debug.msg("start ensureManagedBedrockAt " + rand);

		//Debug.msg("ensureManagedBedrockAt() " + p);
		ManagedBedrockBase managedBedrock = getManagedBedrock(world, p);
		if (managedBedrock == null) {
			//special cases (doors)
			Material mat = getMaterial(world, p);
			//Debug.msg("ensureManagedBedrockAt() getMaterial(): " + mat);
			if (mat == null) mat = p.getType(world);
			boolean isTallDoor = Blocks.isTallDoor(mat);
			boolean isTrapDoor = Blocks.isTrapDoor(mat);
			if (isTallDoor || isTrapDoor) {
				//Debug.msg("ensuring door: " + mat);
				if (isTallDoor) {
					Pair<Point, Point> doorTopBottom = getDoorTopBottom(world, p);
					if (doorTopBottom != null) {
						Point top = doorTopBottom.getKey();
						Point bottom = doorTopBottom.getValue();

						managedBedrock = new ManagedBedrockDoor(world, top, bottom);
						putManagedBedrock(world, top, managedBedrock);
						putManagedBedrock(world, bottom, managedBedrock);
						//Debug.msg("ensureManagedBedrockAt() top: " + top + " (tall door)");
						//Debug.msg("ensureManagedBedrockAt() bottom: " + bottom);
					} //else fallback
				} else { //isTrapDoor
					managedBedrock = new ManagedBedrockDoor(world, p, null);
					putManagedBedrock(world, p, managedBedrock);
					//Debug.msg("ensureManagedBedrockAt() p: " + p + " (trap door)");
				}
			}
			//else Debug.msg("ensuring non door: " + mat);

			//fallback
			if (managedBedrock == null) {
				managedBedrock = new ManagedBedrock(world, p);
				putManagedBedrock(world, p, managedBedrock);
			}
		}

//		Debug.msg("end ensureManagedBedrockAt " + rand);
		return managedBedrock;
	}

	private synchronized ManagedBedrockBase getManagedBedrock(World world, Point p) {
//		Random random = new Random();
//		int rand = random.nextInt(10000);
//		Debug.msg("start getManagedBedrock " + rand);

		ManagedBedrockBase managedBedrock = null;

		Map<Point, ManagedBedrockBase> managedBedrockMap = instance.model.managedBedrockMapByWorld.get(world.getName());
		if (managedBedrockMap != null) {
			managedBedrock = managedBedrockMap.get(p);
		}

//		Debug.msg("end getManagedBedrock " + rand);
		return managedBedrock;
	}

	private synchronized void putManagedBedrock(World world, Point p, ManagedBedrockBase managedBedrock) {
//		Random random = new Random();
//		int rand = random.nextInt(10000);
//		Debug.msg("start putManagedBedrock " + rand);

		//Debug.msg("putManagedBedrock() " + p);
		Map<Point, ManagedBedrockBase> managedBedrockMap = instance.model.managedBedrockMapByWorld.get(world.getName());
		if (managedBedrockMap == null) {
			managedBedrockMap = new HashMap<>();
			instance.model.managedBedrockMapByWorld.put(world.getName(), managedBedrockMap);
		}

		managedBedrockMap.put(p, managedBedrock);

//		Debug.msg("end putManagedBedrock " + rand);
	}

	private synchronized void removeManagedBedrock(World world, Point p) {
//		Random random = new Random();
//		int rand = random.nextInt(10000);
//		Debug.msg("start putManagedBedrock " + rand);

		//Debug.msg("removeManagedBedrock() " + p);
		Map<Point, ManagedBedrockBase> managedBedrockMap = instance.model.managedBedrockMapByWorld.get(world.getName());
		if (managedBedrockMap != null) {
			managedBedrockMap.remove(p);
			if (managedBedrockMap.size() <= 0) {
				instance.model.managedBedrockMapByWorld.remove(world.getName());
			}
		}

//		Debug.msg("end putManagedBedrock " + rand);
	}
}
