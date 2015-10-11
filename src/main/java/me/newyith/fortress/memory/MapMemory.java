package me.newyith.fortress.memory;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapMemory extends AbstractMemory<Map<String, Object>> {
	public MapMemory() {
		this(new LinkedHashMap<>());
	}

	public MapMemory(Map<String, Object> data) {
		super(data);
	}

	public Map<String, Object> section(String sectionName) {
		final Object section = data.get(sectionName);
		if (section instanceof Map) {
			return (Map<String, Object>) section;
		} else {
			Map<String, Object> newSection = new LinkedHashMap<>();
			data.put(sectionName, newSection);
			return newSection;
		}
	}

	// --- PRIMITIVE SAVE/LOAD ---

	//boolean
	public void save(String key, boolean value) {
		data.put(key, value);
	}
	public boolean loadBoolean(String key) { return data.containsKey(key) ? (Boolean) data.get(key) : false; }

	//int
	public void save(String key, int value) {
		data.put(key, value);
	}
	public int loadInt(String key) {
		return data.containsKey(key) ? (Integer) data.get(key) : 0;
	}

	//long
	public void save(String key, long value) {
		data.put(key, value);
	}
	public long loadLong(String key) {
		return data.containsKey(key) ? (Long) data.get(key) : 0;
	}

	//String
	public void save(String key, String value) {
		data.put(key, value);
	}
	public String loadString(String key) {
		return (String) data.get(key);
	}

	@Override
	AbstractMemory<Map<String, Object>> newMemory(Map<String, Object> data) {
		return new MapMemory(data);
	}

}
