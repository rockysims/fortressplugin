package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.core.util.BlockRevertData;
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
		private LinkedList<Set<Point>> waveLayers = null;
		private String worldName = null;
		private transient World world = null;
		private final transient int maxWaveLayers;

		@JsonCreator
		public Model(@JsonProperty("waveLayers") LinkedList<Set<Point>> waveLayers,
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
		LinkedList<Set<Point>> waveLayers = new LinkedList<>();
		String worldName = world.getName();
		model = new Model(waveLayers, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public void convertLayer(Set<Point> layerPoints) {
		//consider removing old layer
		if (model.waveLayers.size() + 1 > model.maxWaveLayers) {
			revertLayer();
		}

		//add new layer
		Set<Point> newLayer = new HashSet<>();
		for (Point p : layerPoints) {
			if (this.contains(p)) continue;
			BedrockManager.convert(model.world, p);
			newLayer.add(p);
		}
		model.waveLayers.add(newLayer);
	}

	public boolean revertLayer() {
		boolean reverted = false;

		if (!model.waveLayers.isEmpty()) {
			Set<Point> layer = model.waveLayers.removeFirst();
			for (Point p : layer) {
				BedrockManager.revert(model.world, p);
			}

			reverted = true;
		}

		return reverted;
	}

	public void revertPoint(Point p) {
		for (Set<Point> waveLayer : model.waveLayers) {
			if (waveLayer.contains(p)) {
				BedrockManager.revert(model.world, p);
				waveLayer.remove(p);
				break;
			}
		}
	}

	public boolean contains(Point p) {
		for (Set<Point> waveLayer : model.waveLayers) {
			if (waveLayer.contains(p)) {
				return true;
			}
		}
		return false;
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
