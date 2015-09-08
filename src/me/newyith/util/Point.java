package me.newyith.util;

import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Point implements Memorable {
	public World world;
	public double x, y, z;

	public void saveTo(Memory m) {
		//*
		String s = "";
		s += world.getName();
		s += ",";
		s += (int) x;
		s += ",";
		s += (int) y;
		s += ",";
		s += (int) z;
		m.save("s", s);
		/*/
		m.save("worldName", world.getName());
		m.save("x", (int) x);
		m.save("y", (int) y);
		m.save("z", (int) z);
		//*/
	}

	public static Point loadFrom(Memory m) {
		//*
		String s = m.loadString("s");
		String[] data = s.split(",");
		World world = Bukkit.getWorld(data[0]);
		int x = Integer.valueOf(data[1]);
		int y = Integer.valueOf(data[2]);
		int z = Integer.valueOf(data[3]);
		return new Point(world, x, y, z);
		/*/
		String worldName = m.loadString("worldName");
		World world = Bukkit.getWorld(worldName);
		int x = m.loadInt("x");
		int y = m.loadInt("y");
		int z = m.loadInt("z");
		return new Point(world, x, y, z);
		//*/
	}

	//------------------------------------------------------------------------------------------------------------------

	public Point(Point p) {
		this.world = p.world;
		this.x = p.x;
		this.y = p.y;
		this.z = p.z;
	}

	public Point(Location loc) {
		this.world = loc.getWorld();
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
	}

	public Point(World world, double x, double y, double z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public World getWorld() {
		return world;
	}

	public Block getBlock() {
		return world.getBlockAt((int)x, (int)y, (int)z);
	}

	public boolean matches(Material material) {
		return this.getBlock().getType() == material;
	}

	public Location toLocation() {
		return new Location(this.world, this.x, this.y, this.z);
	}

	public Point difference(Point p) {
		Point d = new Point(this);
		d.x -= p.x;
		d.y -= p.y;
		d.z -= p.z;
		return d;
	}

	public void add(Point p) {
		if(p != null && p.getWorld() == this.getWorld()) {
			this.x += p.x;
			this.y += p.y;
			this.z += p.z;
		} else {
			throw new IllegalArgumentException("Cannot add Locations of differing worlds");
		}
	}

	public Point add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public boolean is(Material mat) {
		return getBlock().getType() == mat;
	}

	@Override
	public String toString() {
		int x = (int)this.x;
		int y = (int)this.y;
		int z = (int)this.z;

		return x + ", " + y + ", " + z;
	}

	public String toStringDoubles() {
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
