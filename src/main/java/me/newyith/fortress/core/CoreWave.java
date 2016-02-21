package me.newyith.fortress.core;

import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

public class CoreWave {
	private static class Model {
		private LinkedList<WaveLayer> waveLayers = null;
		private String worldName = null;
		private transient World world = null;
		private final transient int maxWaveLayers;

		@JsonCreator
		public Model(@JsonProperty("waveLayers") LinkedList<WaveLayer> waveLayers,
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
		LinkedList<WaveLayer> waveLayers = new LinkedList<>();
		String worldName = world.getName();
		model = new Model(waveLayers, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public void convertLayer(int layerIndex, Set<Point> layerPoints) {
		//consider removing old layer
		if (model.waveLayers.size() + 1 > model.maxWaveLayers) {
			revertLayer();
		}

		//look for oldLayer and remove from waveLayers if found
		WaveLayer oldLayer = null;
		Iterator<WaveLayer> it = model.waveLayers.iterator();
		while (it.hasNext()) {
			WaveLayer layer = it.next();
			if (layer.isLayer(layerIndex)) {
				oldLayer = layer;
				it.remove();
				break;
			}
		}

		//add new layer and convert (and revert oldLayer where needed)
		WaveLayer newLayer = new WaveLayer(model.world, layerIndex, layerPoints);
		newLayer.convert(oldLayer);
		model.waveLayers.add(newLayer);
	}

	public boolean revertLayer() {
		boolean reverted = false;

		if (!model.waveLayers.isEmpty()) {
			WaveLayer layer = model.waveLayers.removeFirst();
			for (Point p : layer.getLayerPoints()) {
				BedrockManager.revert(model.world, p);
			}

			reverted = true;
		}

		return reverted;
	}

	private void revertLayerPoints(Set<Point> layer) {
		for (Point p : layer) {
			BedrockManager.revert(model.world, p);
		}
	}

	public void revertPoint(Point p) {
		for (WaveLayer waveLayer : model.waveLayers) {
			waveLayer.revertPoint(p);
		}
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

