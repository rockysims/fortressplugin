package me.newyith.fortress.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class Point {
//	private static class Model {
//		private final double x;
//		private final double y;
//		private final double z;
//
//		@JsonCreator
//		public Model(@JsonProperty("x") double x,
//					 @JsonProperty("y") double y,
//					 @JsonProperty("y") double z) {
//			this.x = x;
//			this.y = y;
//			this.z = z;
//			onLoaded();
//		}
//
//		private void onLoaded() {
//			//rebuild transient fields
//		}
//	}
//	private Model model = new Model("datum");
//
//	public SandboxThingToSave() {} //dummy constructor for jackson
//
//	@JsonProperty("model")
//	private void setModel(Model model) {
//		this.model = model;
//		model.onLoaded();
//	}
//
//	//-----------------------------------------------------------------------
//








	private Model model;
	public static class Model {
		public final double x;
		public final double y;
		public final double z;

		public Model(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Model() { //dummy constructor for jackson
			x = 0;
			y = 0;
			z = 0;
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

	public Point(int x, int y, int z) {
		this((double)x, (double)y, (double)z);
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

	public Point(Block b) {
		this(b.getLocation());
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
