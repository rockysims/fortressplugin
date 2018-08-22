package me.newyith.fortress.util;

import me.newyith.fortress.main.FortressPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Debug {
	public static boolean showTimerMessages = !FortressPlugin.releaseBuild;

	public static void msg(String s) {
		if (!FortressPlugin.releaseBuild) {
			/*
			Bukkit.broadcastMessage(s);
			/*/
			System.out.println(s);
			//*/
		}
	}

	public static void error(String s) {
		Bukkit.broadcastMessage(ChatColor.RED + "Error: " + s);
	}

	public static void warn(String s) {
		Bukkit.broadcastMessage(ChatColor.YELLOW + "Warning: " + s);
	}

	public static void particleAt(Point point, Particle particle) {
		Player player = getPlayer();
		if (player != null) {
			World world = player.getWorld();
			point = point.add(0.5, 0.5, 0.5);
			Particles.display(particle, 1, world, point, 0);
		}
	}

	public static void particleAtTimed(Point point, Particle particle) {
		int repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(FortressPlugin.getInstance(), () -> {
			particleAt(point, particle);
		}, 0, 20); //delay, period

		Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
			Bukkit.getScheduler().cancelTask(repeatingTaskId);
		}, 20*5); //20 ticks per second
	}
	
	private static Player getPlayer() {
		Player player = null;
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (onlinePlayer.getName().equalsIgnoreCase("newyith")) {
				player = onlinePlayer;
			}
		}
		return player;
	}

	public static void print(String s) {
		Debug.msg(s);
	}

	private static Map<String, Long> timestamps = new HashMap<>();
	private static Map<String, Long> durations = new HashMap<>();

	public static void start(String key) {
		String extraStr = "";
		if (timestamps.containsKey(key)) {
			extraStr = " WAS ALREADY STARTED";
		}

		long now = System.nanoTime();
		timestamps.put(key, now);

		if (showTimerMessages && extraStr.length() > 0) {
			//Debug.print("Timer \"" + key + "\" started." + extraStr);
		}
	}

	public static void end(String key) {
		if (timestamps.containsKey(key) || durations.containsKey(key)) {
			stop(key, false);
			duration(key);
			clear(key);
		} else if (showTimerMessages) {
			Debug.print("Timer \"" + key + "\" ended WITHOUT A DURATION");
		}
	}

	public static void stop(String key) {
		stop(key, true);
	}

	public static void stop(String key, boolean print) {
		long now = System.nanoTime();
		if (timestamps.containsKey(key)) {
			long stamp = timestamps.remove(key);
			long durationNs = now - stamp;

			if (!durations.containsKey(key)) {
				durations.put(key, 0L);
			}
			durations.put(key, durations.get(key) + durationNs);

			if (showTimerMessages && print) Debug.print("Timer " + key + ": " + String.valueOf(durationNs / 1000000) + "ms.");
		} else {
			if (showTimerMessages && print) Debug.print("Timer \"" + key + "\" stopped WITHOUT HAVING BEEN STARTED");
		}
	}

	public static void clear(String key) {
		durations.remove(key);
	}

	public static void duration(String key) {
		long durationNs = durations.get(key);
		if (showTimerMessages) {
			Debug.print("Timer '" + key + "' total duration: " + String.valueOf((durationNs / 1000) / 1000F) + "ms.");
		}
	}

	public static void console(String s) {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		console.sendMessage(s);
	}

}
