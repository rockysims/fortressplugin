package me.newyith.util

import org.bukkit.Bukkit
import org.bukkit.ChatColor

class Log {
	companion object {
		fun success(s: String) {
			sendConsole(s, ChatColor.GREEN)
		}

		fun warn(s: String) {
			sendConsole(s, ChatColor.YELLOW)
		}

		fun error(s: String) {
			sendConsole(s, ChatColor.RED)
		}

		fun log(s: String) {
			sendConsole(s, ChatColor.WHITE)
		}

		fun sendConsole(s: String, color: ChatColor) {
			var s = s
			s = ChatColor.AQUA.toString() + "[FP] " + color + s
			val console = Bukkit.getServer().consoleSender
			console.sendMessage(s)
		}
	}
}