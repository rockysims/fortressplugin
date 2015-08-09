package me.newyith.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Debug {
	public static void msg(String s) {
		Bukkit.broadcastMessage(s);
	}

	public static void error(String s) {
		Bukkit.broadcastMessage(ChatColor.RED + s);
	}
}
