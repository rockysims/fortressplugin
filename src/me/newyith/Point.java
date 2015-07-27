package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Point extends Location {
	public World world;
	public double x, y, z;

	public Point(Location loc) {
		super(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
		this.world = loc.getWorld();
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
	}

	public Point(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean matches(Material material) {
		return this.getBlock().getType() == material;
	}

	@Override
	public String toString() {
		int x = (int)this.x;
		int y = (int)this.y;
		int z = (int)this.z;

		return x + ", " + y + ", " + z;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

        if (o instanceof Point) {
			Point p = (Point)o;

			boolean equal = true;
			equal = equal && (int)this.x == (int)p.x;
			equal = equal && (int)this.y == (int)p.y;
			equal = equal && (int)this.z == (int)p.z;

			return equal;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = (int)x;
		hash = 49999 * hash + (int)y;
		hash = 49999 * hash + (int)z;
		return hash;
	}
}
