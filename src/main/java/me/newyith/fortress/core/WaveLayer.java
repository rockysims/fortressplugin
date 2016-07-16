package me.newyith.fortress.core;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WaveLayer {
	private static class Model {
		private int layerIndex;
		private Set<Point> layerPoints;
		private String worldName = null;
		private transient World world = null;

		@JsonCreator
		public Model(@JsonProperty("layerIndex") int layerIndex,
					 @JsonProperty("layerPoints") Set<Point> layerPoints,
					 @JsonProperty("worldName") String worldName) {
			this.layerIndex = layerIndex;
			this.layerPoints = layerPoints;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public WaveLayer(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public WaveLayer(World world, int layerIndex, Set<Point> layerPoints) {
		model = new Model(layerIndex, layerPoints, world.getName());
	}

	//-----------------------------------------------------------------------

	public void convertAndCleanup(WaveLayer oldLayer, Set<Point> alteredPoints) {
		Debug.msg("convertAndCleanup() model.layerPoints.size(): " + model.layerPoints.size());

		if (oldLayer != null) {
			//revert any oldPoints not in new points
			Set<Point> oldPoints = new HashSet<>(oldLayer.getLayerPoints());
			oldPoints.removeAll(model.layerPoints);
			oldPoints.removeAll(alteredPoints);
			for (Point p : oldPoints) {
				oldLayer.revertPoint(p);
			}
		}

		for (Point p : model.layerPoints) {
			boolean temp = BedrockManager.convert(model.world, p);
			Debug.msg("convertAndCleanup() converted " + p + " (now converted?: " + temp + ")");
		}
	}

	public boolean isLayer(int layerIndex) {
		return model.layerIndex == layerIndex;
	}

	public void revertPoint(Point p) {
		Debug.msg("revertPoint() called " + p);
		if (model.layerPoints.contains(p)) {
			BedrockManager.revert(model.world, p);
			Debug.msg("reverting Point() " + p);
			model.layerPoints.remove(p);
		}
	}

	public Set<Point> getLayerPoints() {
		return model.layerPoints;
	}
}
