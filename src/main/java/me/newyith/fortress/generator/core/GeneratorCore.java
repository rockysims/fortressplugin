package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorState;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.Point;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class GeneratorCore {
	public static class Model extends BaseModel {
		//anchor
		private transient Point anchor = null;
		private Point.Model anchorModel = null;
		public Point getAnchor() {
			if (anchor == null) {
				anchor = new Point(anchorModel);
			}
			return anchor;
		}
		public void setAnchor(Point p) {
			anchor = p;
			anchorModel = anchor.getModel();
		}

		public Model(Point anchor) {
			setAnchor(anchor);
		}
	}
	private Model model;

	public GeneratorCore(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public GeneratorCore(Point anchor) {
		this.model = new Model(anchor);
	}

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
