package me.newyith.fortress.memory;

import org.bukkit.configuration.ConfigurationSection;

public class YamlMemory extends AbstractMemory<ConfigurationSection> {
	public YamlMemory(ConfigurationSection config) {
		super(config);
	}

	public ConfigurationSection section(String sectionName) {
		ConfigurationSection section = config.getConfigurationSection(sectionName);
		if (section == null) {
			section = config.createSection(sectionName);
		}
		return section;
	}

	// --- PRIMITIVE SAVE/LOAD ---

	//boolean
	public void save(String key, boolean value) {
		config.set(key, value);
	}
	public boolean loadBoolean(String key) {
		return config.getBoolean(key);
	}

	//int
	public void save(String key, int value) {
		config.set(key, value);
	}
	public int loadInt(String key) {
		return config.getInt(key);
	}

	//long
	public void save(String key, long value) {
		config.set(key, value);
	}
	public long loadLong(String key) {
		return config.getLong(key);
	}

	//String
	public void save(String key, String value) {
		config.set(key, value);
	}
	public String loadString(String key) {
		return config.getString(key);
	}

	@Override
	AbstractMemory<ConfigurationSection> newMemory(ConfigurationSection config) {
		return new YamlMemory(config);
	}

}
