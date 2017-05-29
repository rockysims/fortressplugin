package me.newyith.fortress.config

import me.newyith.fortress.main.FortressPlugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

object ConfigManager {
	fun load(plugin: JavaPlugin): ConfigData {
		var glowstoneDustBurnTimeMs = 1000 * 60 * 60
		var stuckDelayMs = 30 * 1000
		var stuckCancelDistance = 4
		var generationRangeLimit = 128
		var generationBlockLimit = 40000 //roughly 125 empty 8x8x8 rooms (6x6x6 air inside)

		val config = plugin.getConfig()
		if (FortressPlugin.releaseBuild) {
			glowstoneDustBurnTimeMs = getOrSetInt(config, "glowstoneDustBurnTimeMs", glowstoneDustBurnTimeMs)
			stuckDelayMs = getOrSetInt(config, "stuckDelayMs", stuckDelayMs)
			stuckCancelDistance = getOrSetInt(config, "stuckCancelDistance", stuckCancelDistance)
			generationRangeLimit = getOrSetInt(config, "generationRangeLimit", generationRangeLimit)
			generationBlockLimit = getOrSetInt(config, "generationBlockLimit", generationBlockLimit)
		}
		plugin.saveConfig()

		return ConfigData(
				glowstoneDustBurnTimeMs,
				stuckDelayMs,
				stuckCancelDistance,
				generationRangeLimit,
				generationBlockLimit
				)
	}

	private fun getOrSetInt(config: FileConfiguration, key: String, defaultValue: Int): Int {
		if (!config.isInt(key)) {
			config.set(key, defaultValue)
		}
		return config.getInt(key)
	}
}