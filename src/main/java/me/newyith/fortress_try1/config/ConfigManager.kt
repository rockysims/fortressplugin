package me.newyith.fortress_try1.config

import me.newyith.fortress_try1.main.FortressPlugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

object ConfigManager {
	fun getDefaults(): ConfigData {
		return ConfigData(
			glowstoneDustBurnTimeMs = 1000 * 60 * 60,
			stuckDelayMs = 30 * 1000,
			stuckCancelDistance = 4,
			generationRangeLimit = 128,
			generationBlockLimit = 40000 //roughly 125 empty 8x8x8 rooms (6x6x6 air inside)
		)
	}

	fun loadOrSave(plugin: JavaPlugin): ConfigData {
		val config = plugin.config

		if (!FortressPlugin.releaseBuild) {
			//force use of default config values as defined below (overwrite config file)
			config.getKeys(false).forEach { config.set(it, 0F) }
		}

		val defaults = getDefaults()
		val configData = ConfigData(
			glowstoneDustBurnTimeMs = getOrSetInt(config, "glowstoneDustBurnTimeMs", defaults.glowstoneDustBurnTimeMs),
			stuckDelayMs = getOrSetInt(config, "stuckDelayMs", defaults.stuckDelayMs),
			stuckCancelDistance = getOrSetInt(config, "stuckCancelDistance", defaults.stuckCancelDistance),
			generationRangeLimit = getOrSetInt(config, "generationRangeLimit", defaults.generationRangeLimit),
			generationBlockLimit = getOrSetInt(config, "generationBlockLimit", defaults.generationBlockLimit)
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