package me.newyith.fortress.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import java.util.HashMap;
import java.util.Map;

public class Log {
	public static ChatColor SUCCESS = ChatColor.GREEN;
	public static ChatColor WARN = ChatColor.YELLOW;
	public static ChatColor ERROR = ChatColor.RED;
	public static ChatColor LOG = ChatColor.WHITE;

	public static void success(String s) {
		sendConsole(s, SUCCESS);
	}

	public static void warn(String s) {
		sendConsole(s, WARN);
	}

	public static void error(String s) {
		sendConsole(s, ERROR);
	}

	public static void log(String s) {
		sendConsole(s, LOG);
	}

	public static void sendConsole(String s, ChatColor color) {
		s = ChatColor.AQUA + "[FP] " + color + s;
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		console.sendMessage(s);
	}

	//---//

	private static Map<String, Long> timestamps = new HashMap<>();
	private static Map<String, Long> durations = new HashMap<>();

	public static void start(String key) {
		timestamps.put(key, System.nanoTime());
	}

	public static String end(String key) {
		long nsStart = timestamps.remove(key);
		long nsEnd = System.nanoTime();
		long nsDuration = nsEnd - nsStart;
		return String.valueOf((nsDuration / (1000000 * 10)) / 100F) + " seconds";
	}
}
