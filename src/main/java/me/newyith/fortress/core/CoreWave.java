package me.newyith.fortress.core;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
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

	public void convertLayer(Set<Point> layerPoints, int layerIndex) {
		Debug.msg("++++++++++++++++ convertLayer() start");
		/*
		Debug.msg("ignoring convertLayer() call");
		/*/
		//consider removing old layer
		if (model.waveLayers.size() + 1 > model.maxWaveLayers) {
			Debug.msg("convertLayer() calling revertLayer()");
			revertLayer();
		}
		else {
			//TODO: remove from model.waveLayers all in layerPoints
//			Point layerPoint = layerPoints.stream().findAny().orElse(null);
//			if (layerPoint != null) {
//				Iterator<Set<Point>> it = model.waveLayers.iterator();
//				while (it.hasNext()) {
//					Set<Point> waveLayer = it.next();
//					if (waveLayer.contains(layerPoint)) {
//						//found waveLayer containing a point from layerPoints so revert waveLayer
//						revertLayerPoints(waveLayer);
//						it.remove();
//						Debug.msg("waveLayers not full but before converting found and reverted matching layer");
//					}
//				}
//			}
		}








		//TODO: fix reverse at < 4 bug: make new layer replace old layer if it exists (search for waveLayer with all matching points)










		//add new layer
		Set<Point> newLayer = new HashSet<>();
		for (Point p : layerPoints) {
			if (this.contains(p)) {
				Debug.msg("ignoring: wave contains(p) so don't add p to newLayer. p: " + p);
//				continue;
			}
			BedrockManager.convert(model.world, p);
			newLayer.add(p);
		}
		model.waveLayers.add(newLayer);
		Debug.msg("newLayer.size(): " + newLayer.size());
		//*/
		Debug.msg("---------------- convertLayer() end");
	}

	public boolean revertLayer() {
		Debug.msg("revertLayer() called");
		/*
		Debug.msg("ignoring revertLayer() call");
		return false;
		/*/
		boolean reverted = false;

		if (!model.waveLayers.isEmpty()) {
			Set<Point> layer = model.waveLayers.removeFirst();
			for (Point p : layer) {
				BedrockManager.revert(model.world, p);
				Debug.msg("reverting " + p);
			}

			reverted = true;
		}

		return reverted;
		//*/
	}

	private void revertLayerPoints(Set<Point> layer) {
		for (Point p : layer) {
			BedrockManager.revert(model.world, p);
			Debug.msg("reverting " + p);
		}
	}

//	public void revertLayers() {
//		while (revertLayer());
//	}

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
//		addDummyWaveLayers();
	}

	public void onBeforeDegenerate() {
		Collections.reverse(model.waveLayers);
//		addDummyWaveLayers();
	}

	//doesn't solve the problem because introduces bugs such as instantLayersRemaining getting set too high
//	private void addDummyWaveLayers() {
//		while (model.waveLayers.size() < model.maxWaveLayers) {
//			model.waveLayers.add(new HashSet<>());
//			Debug.msg("DUMMY LAYER ADDED");
//		}
//	}

	public int layerCount() {
		return model.waveLayers.size();
	}
}


//class Variance {
//	static class Vehicle {}
//	static class WaterVehicle extends Vehicle {}
//	static class Boat extends WaterVehicle {}
//	static class Submarine extends WaterVehicle {}
//	static class LandVehicle extends Vehicle {}
//	static class Car extends LandVehicle {}
//	static class Bike extends LandVehicle {}
//
//	void foo() {
//		this.<LandVehicle>bar(new Car());
//	}
//
//	<T> void bar(T t) {}
//}