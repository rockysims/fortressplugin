package me.newyith.fortress.config

import me.newyith.fortress.main.FortressPlugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

object ConfigManager {
	fun loadOrSave(plugin: JavaPlugin): ConfigData {
		val config = plugin.config

		if (!FortressPlugin.releaseBuild) {
			//force use of default config values as defined below (overwrite config file)
			config.getKeys(false).forEach { config.set(it, 0F) }
		}

		val configData = ConfigData(
			glowstoneDustBurnTimeMs = getOrSetInt(config, "glowstoneDustBurnTimeMs", 1000 * 60 * 60),
			stuckDelayMs = getOrSetInt(config, "stuckDelayMs", 30 * 1000),
			stuckCancelDistance = getOrSetInt(config, "stuckCancelDistance", 4),
			generationRangeLimit = getOrSetInt(config, "generationRangeLimit", 128),
			generationBlockLimit = getOrSetInt(config, "generationBlockLimit", 40000) //roughly 125 empty 8x8x8 rooms (6x6x6 air inside)
		)
		plugin.saveConfig()

		return configData
	}

	private fun getOrSetInt(config: FileConfiguration, key: String, defaultValue: Int): Int {
		if (!config.isInt(key)) {
			config.set(key, defaultValue)
		}
		return config.getInt(key)
	}
}