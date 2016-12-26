package me.newyith.fortress.core;

import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TimedBedrockOld implements Comparable<TimedBedrockOld> {
	private static class Model {
		private int endTick = 0;
		private Point point = null;
		private String worldName = null;
		private transient World world = null;

		@JsonCreator
		public Model(@JsonProperty("endTick") int endTick,
					 @JsonProperty("point") Point point,
					 @JsonProperty("worldName") String worldName) {
			this.endTick = endTick;
			this.point = point;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockOld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockOld(World w, Point p, int endTick) {
		String worldName = w.getName();
		model = new Model(endTick, p, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	@Override
	public int compareTo(TimedBedrockOld otherTimedBedrock) {
		return model.endTick - otherTimedBedrock.getEndTick();
	}

	public void convert() {
		BedrockManagerOld.convert(model.world, model.point);
	}

	public void revert() {
		BedrockManagerOld.revert(model.world, model.point);
	}

	public int getEndTick() {
		return model.endTick;
	}
}
