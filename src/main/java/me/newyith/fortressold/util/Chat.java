package me.newyith.fortressold.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Chat {
	public static void ranged(String msg, Point center, int range) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			double distance = player.getLocation().distance(center.toLocation());
			if (distance <= range) {
				player.sendMessage(msg);
			}
		}
	}
}
