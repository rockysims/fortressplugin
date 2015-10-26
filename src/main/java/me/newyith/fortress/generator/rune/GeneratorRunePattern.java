package me.newyith.fortress.generator.rune;

import me.newyith.fortress.util.BaseModel;
import me.newyith.fortress.util.Modelable;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class GeneratorRunePattern implements Modelable {
	public static class Model extends BaseModel {
		Point.Model anchorPoint = null;

		public Model(Point anchorPoint) {
			this.anchorPoint = anchorPoint.getModel();
		}
	}
	private Model model;
	private Point anchorPoint;
	private World world; //TODO: add model.worldName

	public GeneratorRunePattern(Model model) {
		this.model = model;
		this.anchorPoint = new Point(model.anchorPoint);
		this.world = Bukkit.getWorld(model.worldName);
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public Point getAnchor() {
		return anchorPoint;
	}

	public World getWorld() {
		return world;
	}
}
