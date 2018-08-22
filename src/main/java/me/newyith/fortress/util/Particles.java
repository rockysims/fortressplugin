package me.newyith.fortress.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class Particles {
	public static void display(Particle particle, int count, World world, Point p, double rand) {
		Particles.display(particle, count, world, p, rand, rand, rand);
	}

	public static void display(Particle particle, int count, World world, Point p, double xRand, double yRand, double zRand) {
		Location loc = p.toLocation(world);
		int extra = 0; //usually speed
		world.spawnParticle(particle, loc, count, xRand, yRand, zRand, extra);
	}
}
