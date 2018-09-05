package me.newyith.fortress.bedrock;

import javafx.util.Pair;
import me.newyith.fortress.bedrock.util.ManagedBedrock;
import me.newyith.fortress.bedrock.util.ManagedBedrockBase;
import me.newyith.fortress.bedrock.util.ManagedBedrockDoor;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		if (managedBedrock != null) {
			return managedBedrock.isConverted();
		} else {
			return false;
		}
	}

	public void convert(Point p) {
		ManagedBedrockBase managedBedrock = ensureManagedBedrockAt(p);
		managedBedrock.convert(model.world);
		model.managedBedrockByPoint.put(p, managedBedrock);

		if (managedBedrock instanceof ManagedBedrockDoor) {
			ManagedBedrockDoor managedBedrockDoor = (ManagedBedrockDoor) managedBedrock;
			if (managedBedrockDoor.isTallDoor()) {
				Point top = managedBedrockDoor.getTop();
				Point bottom = managedBedrockDoor.getBottom();
				model.managedBedrockByPoint.put(top, managedBedrock);
				model.managedBedrockByPoint.put(bottom, managedBedrock);
			}
		}
	}

	public void revert(Point p) {
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		if (managedBedrock != null) {
			managedBedrock.revert(model.world);
			model.managedBedrockByPoint.remove(p);

			if (managedBedrock instanceof ManagedBedrockDoor) {
				ManagedBedrockDoor managedBedrockDoor = (ManagedBedrockDoor) managedBedrock;
				if (managedBedrockDoor.isTallDoor()) {
					Point top = managedBedrockDoor.getTop();
					Point bottom = managedBedrockDoor.getBottom();
					model.managedBedrockByPoint.remove(top);
					model.managedBedrockByPoint.remove(bottom);
				}
			}
		}
	}

	public Map<Point, Material> getMaterialByPointMap() {
//		Debug.start("getMaterialByPointMap");

		Map<Point, Material> matByPointMap = new HashMap<>();
		for (Point p : model.managedBedrockByPoint.keySet()) {
			ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
			Material mat = managedBedrock.getMaterial(p);
			matByPointMap.put(p, mat);
		}

//		Debug.end("getMaterialByPointMap");

		return matByPointMap;
	}

	public Material getMaterialOrNull(Point p) {
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		return (managedBedrock == null)?null:managedBedrock.getMaterial(p);
	}

	public Pair<Point, Point> getDoorTopBottom(Point p) {
		//assumes p is a door block
		World world = model.world;
		Point top = null;
		Point bottom = null;
		Point a = p.add(0, 1, 0);
		Point b = p.add(0, -1, 0);

		Material above = getMaterialOrNull(a);
		Material below = getMaterialOrNull(b);
		Material middle = getMaterialOrNull(p);
		if (above == null) above = a.getType(world);
		if (below == null) below = b.getType(world);
		if (middle == null) middle = p.getType(world);

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

	// utils //

	private ManagedBedrockBase ensureManagedBedrockAt(Point p) {
		ManagedBedrockBase managedBedrock = model.managedBedrockByPoint.get(p);
		if (managedBedrock == null) {
			Material mat = p.getType(model.world);

			if (Blocks.isTrapDoor(mat)) { // trap door
				managedBedrock = new ManagedBedrockDoor(model.world, p, null);
				model.managedBedrockByPoint.put(p, managedBedrock);
			}
			else if (Blocks.isTallDoor(mat)) { // tall door
				Pair<Point, Point> doorTopBottom = getDoorTopBottom(p);
				if (doorTopBottom != null) {
					Point top = doorTopBottom.getKey();
					Point bottom = doorTopBottom.getValue();

					managedBedrock = new ManagedBedrockDoor(model.world, top, bottom);
					model.managedBedrockByPoint.put(top, managedBedrock);
					model.managedBedrockByPoint.put(bottom, managedBedrock);
				} //else fallback
			}

			//fallback
			if (managedBedrock == null) {
				managedBedrock = new ManagedBedrock(model.world, p);
				model.managedBedrockByPoint.put(p, managedBedrock);
			}
		}

		return managedBedrock;
	}
}
