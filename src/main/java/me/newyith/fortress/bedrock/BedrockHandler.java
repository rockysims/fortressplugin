package me.newyith.fortress.bedrock;

import javafx.util.Pair;
import me.newyith.fortress.core.util.ManagedBedrock;
import me.newyith.fortress.core.util.ManagedBedrockBase;
import me.newyith.fortress.core.util.ManagedBedrockDoor;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class BedrockHandler {
	private static class Model {
		private final Map<Point, ManagedBedrockBase> managedBedrockByPoint;
		private final String worldName;
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("managedBedrockByPoint") Map<Point, ManagedBedrockBase> managedBedrockByPoint,
					 @JsonProperty("worldName") String worldName) {
			this.managedBedrockByPoint = managedBedrockByPoint;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockHandler(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockHandler(World world) {
		model = new Model(new HashMap<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	public boolean isConverted(Point p) {
		return false; //TODO: write
	}

	public Material convert(Point p) {
		ManagedBedrockBase managedBedrock = ensureManagedBedrockAt(p);

		if (!managedBedrock.isConverted()) {
			managedBedrock.convert(model.world);
		}

		return managedBedrock.getMaterial(p);
	}

	public void revert(Point p) {
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		if (managedBedrock != null) {
			managedBedrock.revert(model.world, false);
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

//		BedrockManager.fullRevert(model.world, p); //TODO: delete and replace this line
	}




	private ManagedBedrockBase ensureManagedBedrockAt(Point p) {
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		if (managedBedrock == null) {
			//special cases (doors)
			Material mat = p.getType(model.world);
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
				managedBedrock = new ManagedBedrock(model.world, p);
				model.managedBedrockByPoint.put(p, managedBedrock);
			}
		}

		return managedBedrock;
	}

	public Map<Point, Material> getMaterialByPointMap() {
		return model.materialByPoint;
	}

	public Material getMaterial(Point p) {
		return model.materialByPoint.get(p);
	}


	// utils //

	private Pair<Point, Point> getDoorTopBottom(Point p) {
		//assumes p is a door block
		World world = model.world;
		Point top = null;
		Point bottom = null;
		Point a = p.add(0, 1, 0);
		Point b = p.add(0, -1, 0);
		Material above = a.getType(world);
		Material below = b.getType(world);
		Material middle = p.getType(world);

		if (isConverted(world, a)) above = getMaterial(a);
		if (isConverted(world, b)) below = getMaterial(b);
		if (isConverted(world, p)) middle = getMaterial(p);

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
}
