package me.newyith.fortress.generator.core;

import javafx.util.Pair;
import me.newyith.fortress.generator.BlockRevertData;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

public class CoreWave {
	private static class Model {
		private LinkedList<Map<Point, BlockRevertData>> waveLayers = null;
		private String worldName = null;
		private transient World world = null;
		private final transient int maxWaveLayers;

		@JsonCreator
		public Model(@JsonProperty("waveLayers") LinkedList<Map<Point, BlockRevertData>> waveLayers,
					 @JsonProperty("worldName") String worldName) {
			this.waveLayers = waveLayers;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.maxWaveLayers = 4;
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreWave(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public CoreWave(World world) {
		LinkedList<Map<Point, BlockRevertData>> waveLayers = new LinkedList<>();
		String worldName = world.getName();
		model = new Model(waveLayers, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public boolean revertLayer() {
		boolean reverted = false;

		if (!model.waveLayers.isEmpty()) {
			Map<Point, BlockRevertData> layer = model.waveLayers.removeFirst();
			for (Point p : layer.keySet()) {
				layer.get(p).revert(model.world, p);
			}
			reverted = true;
		}

		return reverted;
	}

	public void convertLayer(Set<Point> layerPoints) {
		//consider removing old layer
		if (model.waveLayers.size() + 1 > model.maxWaveLayers) {
			revertLayer();
		}

		//add new layer
		World world = model.world;
		Map<Point, BlockRevertData> newLayerData = new HashMap<>();
		for (Point p : layerPoints) {
			Material mat = p.getBlock(world).getType();
			if (Blocks.isTallDoor(mat)) {
				convertDoor(p, newLayerData);
			} else {
				newLayerData.put(p, new BlockRevertData(world, p));
				p.getBlock(model.world).setType(Material.BEDROCK);
			}
		}
		model.waveLayers.add(newLayerData);
	}

	public void revertPoint(Point p) {
		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			BlockRevertData data = waveLayer.get(p);
			if (data != null) {
				if (Blocks.isTallDoor(data.getMaterial())) {
					Debug.msg("revertPoint() calling revertDoor() at " + p);
					revertDoor(p, waveLayer);
				} else {
					data.revert(model.world, p);
					waveLayer.remove(p);
				}
				break;
			}
		}
	}
	



	//TODO: delete (or use)
	private BlockRevertData get(Point p) {
		BlockRevertData data = null;
		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			data = waveLayer.get(p);
			if (data != null) break;
		}
		return data;
	}

	//TODO: delete (or use)
	private boolean contains(Point p) {
		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			if (waveLayer.containsKey(p)) {
				return true;
			}
		}
		return false;
	}




	private void convertDoor(Point p, Map<Point, BlockRevertData> layer) {
		//assumes p is a door block (2 block tall doors)
		Pair<Point, Point> doorTopBottom = getDoorTopBottom(p, layer);
		if (doorTopBottom != null) {
			World world = model.world;
			Point top = doorTopBottom.getKey();
			Point bottom = doorTopBottom.getValue();

			layer.put(top, new BlockRevertData(world, top));
			layer.put(bottom, new BlockRevertData(world, bottom));

			bottom.setType(Material.AIR, world);
			top.setType(Material.AIR, world);

			bottom.setType(Material.BEDROCK, world);
			top.setType(Material.BEDROCK, world);
		}
	}

	private void revertDoor(Point p, Map<Point, BlockRevertData> layer) {
		//assumes p is a door block
		Pair<Point, Point> doorTopBottom = getDoorTopBottom(p, layer);
		if (doorTopBottom != null) {
			World world = model.world;
			Point top = doorTopBottom.getKey();
			Point bottom = doorTopBottom.getValue();
			BlockRevertData topData = layer.get(top);
			BlockRevertData bottomData = layer.get(bottom);

			if (topData != null && bottomData != null) {
				bottom.setType(Material.AIR, world);
				top.setType(Material.AIR, world);

				bottomData.revert(world, bottom);
				topData.revert(world, top);

				layer.remove(top);
				layer.remove(bottom);
			} else {
				Debug.error("CoreWave::revertDoor() failed to find revert data for door's top and bottom.");
			}
		}
	}

	private Pair<Point, Point> getDoorTopBottom(Point p, Map<Point, BlockRevertData> layer) {
		//assumes p is a door block
		Point top = null;
		Point bottom = null;
		Point a = p.add(0, 1, 0);
		Point b = p.add(0, -1, 0);
		Material above = a.getType(model.world);
		Material below = b.getType(model.world);
		Material middle = p.getType(model.world);

		if (layer.containsKey(a)) above = layer.get(a).getMaterial();
		if (layer.containsKey(b)) below = layer.get(b).getMaterial();
		if (layer.containsKey(p)) middle = layer.get(p).getMaterial();

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

	public Material getMaterial(Point p) {
		Material material = null;

		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			BlockRevertData data = waveLayer.get(p);
			if (data != null) {
				material = data.getMaterial();
				break;
			}
		}

		return material;
	}

	public Map<Point,Material> getMaterialMap() {
		Map<Point, Material> map = new HashMap<>();

		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			for (Point p : waveLayer.keySet()) {
				BlockRevertData data = waveLayer.get(p);
				map.put(p, data.getMaterial());
			}
		}

		return map;
	}

	public void onBeforeGenerate() {
		Collections.reverse(model.waveLayers);
	}

	public void onBeforeDegenerate() {
		Collections.reverse(model.waveLayers);
	}

	public int layerCount() {
		return model.waveLayers.size();
	}
}
