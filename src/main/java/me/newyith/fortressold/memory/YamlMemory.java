package me.newyith.fortressold.memory;

import org.bukkit.configuration.ConfigurationSection;

public class YamlMemory extends AbstractMemory<ConfigurationSection> {
	public YamlMemory(ConfigurationSection data) {
		super(data);
	}

	public ConfigurationSection section(String sectionName) {
		ConfigurationSection section = data.getConfigurationSection(sectionName);
		if (section == null) {
			section = data.createSection(sectionName);
		}
		return section;
	}

	// --- PRIMITIVE SAVE/LOAD ---

	//boolean
	public void save(String key, boolean value) {
		data.set(key, value);
	}
	public boolean loadBoolean(String key) {
		return data.getBoolean(key);
	}

	//int
	public void save(String key, int value) {
		data.set(key, value);
	}
	public int loadInt(String key) {
		return data.getInt(key);
	}

	//long
	public void save(String key, long value) {
		data.set(key, value);
	}
	public long loadLong(String key) {
		return data.getLong(key);
	}

	//String
	public void save(String key, String value) {
		data.set(key, value);
	}
	public String loadString(String key) {
		return data.getString(key);
	}

	@Override
	AbstractMemory<ConfigurationSection> newMemory(ConfigurationSection data) {
		return new YamlMemory(data);
	}

}
