package me.newyith.fortress.memory;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapMemory extends AbstractMemory<Map<String, Object>> {
	public MapMemory() {
		this(new LinkedHashMap<>());
	}

	public MapMemory(Map<String, Object> config) {
		super(config);
	}

	public Map<String, Object> section(String sectionName) {
		final Object section = config.get(sectionName);
		if (section instanceof Map) {
			return (Map<String, Object>) section;
		} else {
			Map<String, Object> newSection = new LinkedHashMap<>();
			config.put(sectionName, newSection);
			return newSection;
		}
	}

	// --- PRIMITIVE SAVE/LOAD ---

	//boolean
	public void save(String key, boolean value) {
		config.put(key, value);
	}
	public boolean loadBoolean(String key) { return config.containsKey(key) ? (Boolean) config.get(key) : false; }

	//int
	public void save(String key, int value) {
		config.put(key, value);
	}
	public int loadInt(String key) {
		return config.containsKey(key) ? (Integer) config.get(key) : 0;
	}

	//long
	public void save(String key, long value) {
		config.put(key, value);
	}
	public long loadLong(String key) {
		return config.containsKey(key) ? (Long) config.get(key) : 0;
	}

	//String
	public void save(String key, String value) {
		config.put(key, value);
	}
	public String loadString(String key) {
		return (String) config.get(key);
	}

	@Override
	AbstractMemory<Map<String, Object>> newMemory(Map<String, Object> config) {
		return new MapMemory(config);
	}

}
