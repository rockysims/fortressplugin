package me.newyith.fortress.util

import org.bukkit.Bukkit
import org.bukkit.ChatColor

object Log {
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
		val msg = ChatColor.AQUA.toString() + "[FP] " + color + s
		val console = Bukkit.getServer().consoleSender
		console.sendMessage(msg)
	}
}