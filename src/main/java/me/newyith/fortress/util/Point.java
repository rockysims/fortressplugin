package me.newyith.fortress.util;

import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.model.Modelable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class Point implements Modelable {
	private Model model;
	public static class Model extends BaseModel {
		public final double x;
		public final double y;
		public final double z;

		public Model(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	public Model getModel() {
		return this.model;
	}

	public Point(Model model) {
		this.model = model;
	}

	//-----------------------------------------------------------------------

	// - Constructors - //

	public Point(double x, double y, double z) {
		model = new Model(x, y, z);
	}

	public Point(Point p) {
		model = p.getModel();
	}

	public Point(Location loc) {
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		model = new Model(x, y, z);
	}

	public Point(Vector vec) {
		double x = vec.getX();
		double y = vec.getY();
		double z = vec.getZ();
		model = new Model(x, y, z);
	}

	// - Getters / Setters - //

	public double x() {
		return model.x;
	}
	public double y() {
		return model.y;
	}
	public double z() {
		return model.z;
	}
	public int xInt() {
		return (int) Math.floor(model.x);
	}
	public int yInt() {
		return (int) Math.floor(model.y);
	}
	public int zInt() {
		return (int) Math.floor(model.z);
	}

	// - Public Utils - //

	public Location toLocation(World world) {
		return new Location(world, x(), y(), z());
	}

	public Vector toVector() {
		return new Vector(x(), y(), z());
	}

	public Point add(double xAdd, double yAdd, double zAdd) {
		double x = x() + xAdd;
		double y = y() + yAdd;
		double z = z() + zAdd;
		return new Point(x, y, z);
	}

	public boolean is(Material mat, World world) {
		return getBlock(world).getType() == mat;
	}

	public Block getBlock(World world) {
		return world.getBlockAt(xInt(), yInt(), zInt());
	}

	// - Overrides - //

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof Point) {
			Point p = (Point)o;

			boolean equal = true;
			equal = equal && Math.floor(x()) == Math.floor(p.x());
			equal = equal && Math.floor(y()) == Math.floor(p.y());
			equal = equal && Math.floor(z()) == Math.floor(p.z());

			return equal;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = (int) Math.floor(x());
		hash = 49999 * hash + (int) Math.floor(y());
		hash = 49999 * hash + (int) Math.floor(z());
		return hash;
	}
}
