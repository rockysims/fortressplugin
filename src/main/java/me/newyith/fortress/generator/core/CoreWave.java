package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.BlockRevertData;
import me.newyith.fortress.util.Point;
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
		Map<Point, BlockRevertData> newLayerData = new HashMap<>();
		for (Point p : layerPoints) {
			newLayerData.put(p, new BlockRevertData(model.world, p));
			p.getBlock(model.world).setType(Material.QUARTZ_BLOCK); //TODO: change to BEDROCK
		}
		model.waveLayers.add(newLayerData);
	}

	public boolean revertPoint(Point p) {
		boolean reverted = false;

		for (Map<Point, BlockRevertData> waveLayer : model.waveLayers) {
			BlockRevertData data = waveLayer.get(p);
			if (data != null) {
				data.revert(model.world, p);
				waveLayer.remove(p);
				reverted = true;
				break;
			}
		}

		return reverted;
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
