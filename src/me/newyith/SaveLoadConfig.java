package me.newyith;

import org.bukkit.configuration.ConfigurationSection;

public interface SaveLoadConfig {
	public void saveToConfig(ConfigurationSection config);
	public void loadFromConfig(ConfigurationSection config);
}
