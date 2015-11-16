package me.newyith.fortress.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;

import java.util.*;

public class Cuboid implements Cloneable, ConfigurationSerializable, Iterable<Block> {
	protected final String worldName;
	protected World world;
	protected final Vector minimumPoint, maximumPoint;

	public static Cuboid fromCuboids(List<Cuboid> cuboids, World world) {
		Cuboid resultCuboid = null;

		if (cuboids.size() > 0) {
			//init bestMin and bestMax points
			Cuboid firstCuboid = cuboids.get(0);
			Vector bestMin = new Point(firstCuboid.minimumPoint).toVector();
			Vector bestMax = new Point(firstCuboid.maximumPoint).toVector();

			//find bestMin and bestMax points
			Iterator<Cuboid> it = cuboids.iterator();
			while (it.hasNext()) {
				Cuboid cuboid = it.next();
				Point cubMin = new Point(cuboid.minimumPoint);
				Point cubMax = new Point(cuboid.maximumPoint);

				//update bestMin
				bestMin.setX(Math.min(bestMin.getX(), cubMin.x()));
				bestMin.setY(Math.min(bestMin.getY(), cubMin.y()));
				bestMin.setZ(Math.min(bestMin.getZ(), cubMin.z()));

				//update bestMax
				bestMax.setX(Math.max(bestMax.getX(), cubMax.x()));
				bestMax.setY(Math.max(bestMax.getY(), cubMax.y()));
				bestMax.setZ(Math.max(bestMax.getZ(), cubMax.z()));
			}

			resultCuboid = new Cuboid(bestMin, bestMax, world);
		}

		return resultCuboid;
	}

	public Cuboid(Cuboid cuboid) {
		this(cuboid.worldName, cuboid.minimumPoint.getX(), cuboid.minimumPoint.getY(), cuboid.minimumPoint.getZ(), cuboid.maximumPoint.getX(), cuboid.maximumPoint.getY(), cuboid.maximumPoint.getZ());
	}

	public Cuboid(Location loc) {
		this(loc, loc);
	}

	public Cuboid(Vector v1, Vector v2, World world) {
		this(new Point(v1), new Point(v2), world);
	}

	public Cuboid(Point p1, Point p2, World world) {
		this(p1.toLocation(world), p2.toLocation(world));
	}

	public Cuboid(Location loc1, Location loc2) {
		if (loc1 != null && loc2 != null) {
			if (loc1.getWorld() != null && loc2.getWorld() != null) {
				if (!loc1.getWorld().getUID().equals(loc2.getWorld().getUID()))
					throw new IllegalStateException("The 2 locations of the cuboid must be in the same world!");
			} else {
				throw new NullPointerException("One/both of the worlds is/are null!");
			}
			this.worldName = loc1.getWorld().getName();

			double xPos1 = Math.min(loc1.getX(), loc2.getX());
			double yPos1 = Math.min(loc1.getY(), loc2.getY());
			double zPos1 = Math.min(loc1.getZ(), loc2.getZ());
			double xPos2 = Math.max(loc1.getX(), loc2.getX());
			double yPos2 = Math.max(loc1.getY(), loc2.getY());
			double zPos2 = Math.max(loc1.getZ(), loc2.getZ());
			this.minimumPoint = new Vector(xPos1, yPos1, zPos1);
			this.maximumPoint = new Vector(xPos2, yPos2, zPos2);
		} else {
			throw new NullPointerException("One/both of the locations is/are null!");
		}
	}

	public Cuboid(String worldName, double x1, double y1, double z1, double x2, double y2, double z2) {
		if (worldName == null || Bukkit.getServer().getWorld(worldName) == null)
			throw new NullPointerException("One/both of the worlds is/are null!");
		this.worldName = worldName;

		double xPos1 = Math.min(x1, x2);
		double xPos2 = Math.max(x1, x2);
		double yPos1 = Math.min(y1, y2);
		double yPos2 = Math.max(y1, y2);
		double zPos1 = Math.min(z1, z2);
		double zPos2 = Math.max(z1, z2);
		this.minimumPoint = new Vector(xPos1, yPos1, zPos1);
		this.maximumPoint = new Vector(xPos2, yPos2, zPos2);
	}

	public Cuboid(World world, Set<Point> points) {
//		Debug.start("CuboidFromPoints");
		if (points.isEmpty()) {
			throw new RuntimeException("Failed to create cuboid because points.isEmpty().");
		} else {
			Point min = points.iterator().next();
			Point max = new Point(min);
			double xMin = min.x();
			double yMin = min.y();
			double zMin = min.z();
			double xMax = max.x();
			double yMax = max.y();
			double zMax = max.z();
			for (Point p : points) {
				xMin = Math.min(xMin, p.x());
				yMin = Math.min(yMin, p.y());
				zMin = Math.min(zMin, p.z());
				xMax = Math.max(xMax, p.x());
				yMax = Math.max(yMax, p.y());
				zMax = Math.max(zMax, p.z());
			}
			minimumPoint = new Vector(xMin, yMin, zMin);
			maximumPoint = new Vector(xMax, yMax, zMax);
			worldName = world.getName();
		}
//		Debug.end("CuboidFromPoints");
	}

	public int countBlocks() {
		Point min = new Point(minimumPoint);
		Point max = new Point(maximumPoint);
		Point diff = min.difference(max);
		int x = Math.abs(diff.xInt()) + 1;
		int y = Math.abs(diff.yInt()) + 1;
		int z = Math.abs(diff.zInt()) + 1;
		int count = x * y * z;
		return count;
	}

	public Point getMin() {
		return new Point(minimumPoint);
	}

	public Point getMax() {
		return new Point(maximumPoint);
	}

	public Set<Point> getPointsAtHeight(double y) {
		Set<Point> points = new HashSet<>();

		Point min = new Point(minimumPoint);
		Point max = new Point(maximumPoint);
		for (double x = min.x(); x <= max.x(); x++) {
			for (double z = min.z(); z <= max.z(); z++) {
				points.add(new Point(x, y, z));
			}
		}

		return points;
	}

	public boolean contains(Point point) {
		return containsLocation(point.toLocation(getWorld()));
	}

	public boolean containsLocation(Location location) {
		return location != null && location.toVector().isInAABB(this.minimumPoint, this.maximumPoint);
	}

	public boolean containsVector(Vector vector) {
		return vector != null && vector.isInAABB(this.minimumPoint, this.maximumPoint);
	}

	public List<Block> getBlocks() {
		List<Block> blockList = new ArrayList<>();
		World world = this.getWorld();
		if (world != null) {
			for (int x = this.minimumPoint.getBlockX(); x <= this.maximumPoint.getBlockX(); x++) {
				for (int y = this.minimumPoint.getBlockY(); y <= this.maximumPoint.getBlockY() && y <= world.getMaxHeight(); y++) {
					for (int z = this.minimumPoint.getBlockZ(); z <= this.maximumPoint.getBlockZ(); z++) {
						blockList.add(world.getBlockAt(x, y, z));
					}
				}
			}
		}
		return blockList;
	}

	public Location getLowerLocation() {
		return this.minimumPoint.toLocation(this.getWorld());
	}

	public double getLowerX() {
		return this.minimumPoint.getX();
	}

	public double getLowerY() {
		return this.minimumPoint.getY();
	}

	public double getLowerZ() {
		return this.minimumPoint.getZ();
	}

	public Location getUpperLocation() {
		return this.maximumPoint.toLocation(this.getWorld());
	}

	public double getUpperX() {
		return this.maximumPoint.getX();
	}

	public double getUpperY() {
		return this.maximumPoint.getY();
	}

	public double getUpperZ() {
		return this.maximumPoint.getZ();
	}

	public double getVolume() {
		return (this.getUpperX() - this.getLowerX() + 1) * (this.getUpperY() - this.getLowerY() + 1) * (this.getUpperZ() - this.getLowerZ() + 1);
	}

	public World getWorld() {
		if (world == null) {
			world = Bukkit.getServer().getWorld(worldName);
		}

		if (world == null) throw new NullPointerException("World '" + this.worldName + "' is not loaded.");
		return world;
	}

	@Override
	public Cuboid clone() {
		return new Cuboid(this);
	}

	@Override
	public ListIterator<Block> iterator() {
		return this.getBlocks().listIterator();
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serializedCuboid = new HashMap<>();
		serializedCuboid.put("worldName", this.worldName);
		serializedCuboid.put("x1", this.minimumPoint.getX());
		serializedCuboid.put("x2", this.maximumPoint.getX());
		serializedCuboid.put("y1", this.minimumPoint.getY());
		serializedCuboid.put("y2", this.maximumPoint.getY());
		serializedCuboid.put("z1", this.minimumPoint.getZ());
		serializedCuboid.put("z2", this.maximumPoint.getZ());
		return serializedCuboid;
	}

	public static Cuboid deserialize(Map<String, Object> serializedCuboid) {
		try {
			String worldName = (String) serializedCuboid.get("worldName");

			double xPos1 = (Double) serializedCuboid.get("x1");
			double xPos2 = (Double) serializedCuboid.get("x2");
			double yPos1 = (Double) serializedCuboid.get("y1");
			double yPos2 = (Double) serializedCuboid.get("y2");
			double zPos1 = (Double) serializedCuboid.get("z1");
			double zPos2 = (Double) serializedCuboid.get("z2");

			return new Cuboid(worldName, xPos1, yPos1, zPos1, xPos2, yPos2, zPos2);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
