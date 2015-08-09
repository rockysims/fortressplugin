package me.newyith.memory;

import me.newyith.generator.FortressGeneratorRune;
import me.newyith.generator.FortressGeneratorRunePattern;
import me.newyith.generator.GeneratorCore;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

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

	//List<Memorable>
	public void save(String key, List list) {
		Memory m = new Memory(section(key));
		int i = 0;
		for (Object item : list) {
			if (item instanceof Memorable) {
				Memorable mem = (Memorable) item;
				m.save(Integer.toString(i++), mem);
			} else if (item instanceof List) {
				List subList = (List) item;
				m.save(Integer.toString(i++), subList);
			} else {
				Debug.error("Memory::save(String, List) failed to save item " + i + ".");
			}
		}
		m.save("count", i);
	}

	//Set<Point>
	public void save(String key, Set<Point> set) {
		Memory m = new Memory(section(key));
		Point[] ary = set.toArray(new Point[set.size()]);
		ArrayList<Point> list = new ArrayList<>(Arrays.asList(ary));
		m.save(key, list);
	}

	//HashMap<Point, Material>
	public void save(String key, HashMap<Point, Material> map) {
		Memory m = new Memory(section(key));

		int i = 0;
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			Memorable itemKey = (Memorable) pair.getKey();
			Enum itemValue = (Enum) pair.getValue();
			m.save("itemKey" + i, itemKey);
			m.save("itemValue" + i, itemValue.ordinal());
			i++;
		}
		m.save("count", i);
	}

	// --- LOAD ---

	//HashMap<Point, Material>
	public HashMap<Point, Material> loadPointMaterialMap(String key) {
		Memory m = new Memory(section(key));

		HashMap<Point, Material> map = new HashMap<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			Point p = m.loadPoint("itemKey" + i);
			Material mat = Material.values()[m.loadInt("itemValue" + i)];
			map.put(p, mat);
		}

		return map;
	}













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
	public ArrayList<Point> loadPointList(String key) {
		Memory m = new Memory(section(key));

		ArrayList<Point> list = new ArrayList<Point>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			list.add(m.loadPoint(Integer.toString(i)));
		}

		return list;
	}

	//List<List<Point>>
	public List<List<Point>> loadLayers(String key) {
		Memory m = new Memory(section(key));

		List<List<Point>> layers = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			layers.add(m.loadPointList(Integer.toString(i)));
		}

		return layers;
	}

	//Set<Point>
	public Set<Point> loadPointSet(String key) {
		Memory m = new Memory(section(key));

		List<Point> list = m.loadPointList(key);
		Set<Point> set = new HashSet<>(list);

		return set;
	}





	//HashMap<Point, Material>






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
	public GeneratorCore loadGeneratorCore(String key) {
		Memory m = new Memory(section(key));

		GeneratorCore value = null;
		if (m.loadBoolean("!=null")) {
			value = GeneratorCore.loadFrom(m);
		}

		return value;
	}








}
