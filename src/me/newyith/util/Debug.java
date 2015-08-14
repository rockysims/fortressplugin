package me.newyith.util;

import me.newyith.particles.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

public class Debug {
	public static void msg(String s) {
		Bukkit.broadcastMessage(s);
	}

	public static void error(String s) {
		Bukkit.broadcastMessage(ChatColor.RED + s);
	}

	public static void particleAt(Point point) {
		float speed = 0;
		int amount = 1;
		double range = 25;
		Location loc = point.add(0.5, -0.5, 0.5);
		ParticleEffect.SNOWBALL.display(0.0F, 0.0F, 0.0F, speed, amount, loc, range);
	}
}
