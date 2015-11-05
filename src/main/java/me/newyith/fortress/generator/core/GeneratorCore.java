package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorState;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.entity.Player;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class GeneratorCore {
	private static class Model {
		private Point anchor = null;

		@JsonCreator
		public Model(@JsonProperty("anchor") Point anchor) {
			this.anchor = anchor;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public GeneratorCore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public GeneratorCore(Point anchor) {
		model = new Model(anchor);
	}

	//-----------------------------------------------------------------------

	public Set<Point> getGeneratedPoints() {
		Debug.msg("//TODO: write GeneratorCore.getGeneratedPoints()");
		return null;
	}

	public Set<Point> getLayerOutsideFortress() {
		//TODO: write
		return new HashSet<>();
	}

	public void tick() {
		//TODO: write
	}

	public boolean onPlaced(Player player) {
		//TODO: write
		return true;
	}

	public void onBroken() {
		//TODO: write
	}

	public void onStateChanged(GeneratorState state) {
		//TODO: write
	}

	public Set<Point> getAlteredPoints() {
		//TODO: write
		return new HashSet<>();
	}

	public Set<Point> getProtectedPoints() {
		//TODO: write
		return new HashSet<>();
	}

	public Set<Point> getClaimedPoints() {
		//TODO: write
		return new HashSet<>();
	}

	public boolean playerCanOpenDoor(Player player, Point doorPoint) {
		//TODO: write
		return false;
	}

}
