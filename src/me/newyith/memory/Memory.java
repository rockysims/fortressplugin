package me.newyith.memory;

import me.newyith.generator.FortressGeneratorRune;
import me.newyith.generator.FortressGeneratorRunePattern;
import me.newyith.generator.GeneratorCore;
import me.newyith.util.Point;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Memory {
	private ConfigurationSection config;

	public Memory(ConfigurationSection config) {
		this.config = config;
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

	// --- SAVE ---

	//Memorable
	public void save(String key, Memorable value) {
		Memory m = new Memory(section(key));
		m.save("!=null", value != null);
		value.saveTo(m);
	}

	//ArrayList<Memorable>
	public void save(String key, List list) {
		Memory m = new Memory(section(key));
		int i = 0;
		for (Object item : list) {
			if (item instanceof Memorable) {
				Memorable mem = (Memorable) item;
				m.save(Integer.toString(i++), mem);
			}
		}
		m.save("count", i);
	}

	// --- LOAD ---

	//Point
	public Point loadPoint(String key) {
		Memory m = new Memory(section(key));

		Point value = null;
		if (m.loadBoolean("!=null")) {
			value = Point.loadFrom(m);
		}

		return value;
	}

	//ArrayList<Point>
	public ArrayList<Point> loadPoints(String key) {
		Memory m = new Memory(section(key));

		ArrayList<Point> list = new ArrayList<Point>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			list.add(m.loadPoint(Integer.toString(i)));
		}

		return list;
	}

	//FortressGeneratorRune
	public FortressGeneratorRune loadFortressGeneratorRune(String key) {
		Memory m = new Memory(section(key));

		FortressGeneratorRune value = null;
		if (m.loadBoolean("!=null")) {
			value = FortressGeneratorRune.loadFrom(m);
		}

		return value;
	}

	//ArrayList<FortressGeneratorRune>
	public ArrayList<FortressGeneratorRune> loadFortressGeneratorRunes(String key) {
		Memory m = new Memory(section(key));

		ArrayList<FortressGeneratorRune> list = new ArrayList<FortressGeneratorRune>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			list.add(m.loadFortressGeneratorRune(Integer.toString(i)));
		}

		return list;
	}

	//FortressGeneratorRunePattern
	public FortressGeneratorRunePattern loadFortressGeneratorRunePattern(String key) {
		Memory m = new Memory(section(key));

		FortressGeneratorRunePattern value = null;
		if (m.loadBoolean("!=null")) {
			value = FortressGeneratorRunePattern.loadFrom(m);
		}

		return value;
	}

//	//ArrayList<FortressGeneratorRunePattern>
//	public ArrayList<FortressGeneratorRunePattern> loadFortressGeneratorRunePatterns(String key) {
//		Memory m = new Memory(section(key));
//
//		ArrayList<FortressGeneratorRunePattern> list = new ArrayList<FortressGeneratorRunePattern>();
//		int count = m.loadInt("count");
//		for (int i = 0; i < count; i++) {
//			list.add(m.loadFortressGeneratorRunePattern(Integer.toString(i)));
//		}
//
//		return list;
//	}



	//FortressGeneratorRunePattern
	public GeneratorCore loadGeneratorCore(String key, FortressGeneratorRune rune) {
		Memory m = new Memory(section(key));

		GeneratorCore value = null;
		if (m.loadBoolean("!=null")) {
			value = GeneratorCore.loadFrom(m, rune);
		}

		return value;
	}








}
