package me.newyith.fortressOrig.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

public class Log {
	public static void success(String s) {
		sendConsole(s, ChatColor.GREEN);
	}

	public static void warn(String s) {
		sendConsole(s, ChatColor.YELLOW);
	}

	public static void error(String s) {
		sendConsole(s, ChatColor.RED);
	}

	public static void log(String s) {
		sendConsole(s, ChatColor.WHITE);
	}

	public static void sendConsole(String s, ChatColor color) {
		s = ChatColor.AQUA + "[FP] " + color + s;
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		console.sendMessage(s);
	}
}
