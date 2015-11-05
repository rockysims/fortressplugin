package me.newyith.fortress.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.text.DecimalFormat;

//fully written again
public class Point {
	private static class Model {
	public final double x;
	public final double y;
	public final double z;

	@JsonCreator
	public Model(@JsonProperty("x") double x,
				 @JsonProperty("y") double y,
				 @JsonProperty("z") double z) {
		this.x = x;
		this.y = y;
		this.z = z;

		//rebuild transient fields
	}
}
	private Model model = null;

	@JsonCreator
	public Point(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public Point(double x, double y, double z) {
		model = new Model(x, y, z);
	}

	public Point(int x, int y, int z) {
		model = new Model(x, y, z);
	}

	public Point(Point p) {
		model = new Model(p.x(), p.y(), p.z());
	}

	public Point(Vector vec) {
		double x = vec.getX();
		double y = vec.getY();
		double z = vec.getZ();
		model = new Model(x, y, z);
	}

	public Point(Location loc) {
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		model = new Model(x, y, z);
	}

	public Point(Block b) {
		this(b.getLocation());
	}

	//-----------------------------------------------------------------------

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

	public Point difference(Point p) {
		double x = x() - p.x();
		double y = y() - p.y();
		double z = z() - p.z();
		return new Point(x, y, z);
	}

	public Point add(Point p) {
		if(p != null) {
			double x = x() + p.x();
			double y = y() + p.y();
			double z = z() + p.z();
			return new Point(x, y, z);
		} else {
			throw new IllegalArgumentException("Point.add() passed null.");
		}
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

	public double distance(Point p) {
		Vector v1 = this.toVector();
		Vector v2 = p.toVector();
		return v1.distance(v2);
	}

	@Override
	public String toString() {
		int x = xInt();
		int y = yInt();
		int z = zInt();

		return x + ", " + y + ", " + z;
	}

	public String toStringDoubles() {
		String format = "#0.00";
		StringBuilder s = new StringBuilder();
		s.append(new DecimalFormat(format).format(model.x));
		s.append(", ");
		s.append(new DecimalFormat(format).format(model.y));
		s.append(", ");
		s.append(new DecimalFormat(format).format(model.z));
		return s.toString();
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
