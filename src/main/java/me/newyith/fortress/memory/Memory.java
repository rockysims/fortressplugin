package me.newyith.fortress.memory;

import me.newyith.fortress.generator.FortressGeneratorRune;
import me.newyith.fortress.generator.FortressGeneratorRunePattern;
import me.newyith.fortress.generator.GeneratorCoreAnimator;
import me.newyith.fortress.generator.GeneratorCore;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
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

	// --- SAVE/LOAD COMPACT ---

	//Set<Point> (compact)
	public void savePointSetCompact(String key, Set<Point> set) {
		Memory m = new Memory(section(key));
		Point[] ary = set.toArray(new Point[set.size()]);
		ArrayList<Point> list = new ArrayList<>(Arrays.asList(ary));
		m.savePointListCompact(key, list);
	}
	public Set<Point> loadPointSetCompact(String key) {
		Memory m = new Memory(section(key));

		List<Point> list = m.loadPointListCompact(key);
		Set<Point> set = new HashSet<>(list);

		return set;
	}

	//List<Point> (compact)
	public void savePointListCompact(String key, List<Point> list) {
		Memory m = new Memory(section(key));

		String blob = "";
		if (list.size() > 0) {
			m.save("world", list.get(0).world.getName());

			for (Point p : list) {
				String pointBlob = "";
				pointBlob += (int) p.x;
				pointBlob += ",";
				pointBlob += (int) p.y;
				pointBlob += ",";
				pointBlob += (int) p.z;
				blob += pointBlob + "~";
			}
			blob = blob.substring(0, blob.length() - 1); //remove last "~"
		}
		m.save("blob", blob);
	}
	public List<Point> loadPointListCompact(String key) {
		Memory m = new Memory(section(key));

		ArrayList<Point> list = new ArrayList<>();
		String blob = m.loadString("blob");
		if (blob.length() > 0) {
			World world = Bukkit.getWorld(m.loadString("world"));
			String[] pointBlobs = blob.split("~");
			for (String pointBlob : pointBlobs) {
				String[] data = pointBlob.split(",");
				int x = Integer.valueOf(data[0]);
				int y = Integer.valueOf(data[1]);
				int z = Integer.valueOf(data[2]);
				list.add(new Point(world, x, y, z));
			}
		}

		return list;
	}

	//List<List<Point>> (compact)
	public void saveLayersCompact(String key, List<List<Point>> layers) {
		Memory m = new Memory(section(key));

		int i = 0;
		for (List<Point> layer : layers) {
			m.savePointListCompact(Integer.toString(i++), layer);
		}
		m.save("count", i);
	}
	public List<List<Point>> loadLayersCompact(String key) {
		Memory m = new Memory(section(key));

		List<List<Point>> layers = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			layers.add(m.loadPointListCompact(Integer.toString(i)));
		}

		return layers;
	}

	//HashMap<Point, Material> (compact)
	public void savePointMaterialMapCompact(String key, HashMap<Point, Material> map) {
		Memory m = new Memory(section(key));

		String blob = "";
		if (map.size() > 0) {
			Point firstKey = map.keySet().iterator().next();
			m.save("world", firstKey.world.getName());

			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				Point p = (Point) pair.getKey();
				Material mat = (Material) pair.getValue();

				String pairBlob = "";
				pairBlob += (int) p.x;
				pairBlob += ",";
				pairBlob += (int) p.y;
				pairBlob += ",";
				pairBlob += (int) p.z;
				pairBlob += ",";
				pairBlob += mat.ordinal();
				blob += pairBlob + "~";
			}
			blob = blob.substring(0, blob.length() - 1); //remove last "~"
		}
		m.save("blob", blob);
	}
	public HashMap<Point, Material> loadPointMaterialMapCompact(String key) {
		Memory m = new Memory(section(key));

		HashMap<Point, Material> map = new HashMap<>();
		String blob = m.loadString("blob");
		if (blob.length() > 0) {
			World world = Bukkit.getWorld(m.loadString("world"));
			String[] pairBlobs = blob.split("~");
			for (String pairBlob : pairBlobs) {
				String[] data = pairBlob.split(",");
				int x = Integer.valueOf(data[0]);
				int y = Integer.valueOf(data[1]);
				int z = Integer.valueOf(data[2]);
				Material mat = Material.values()[Integer.valueOf(data[3])];
				Point p = new Point(world, x, y, z);
				map.put(p, mat);
			}
		}

		return map;
	}

	// --- SAVE ---

	//Memorable
	public void save(String key, Memorable value) {
		Memory m = new Memory(section(key));
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

	//Point
	public Point loadPoint(String key) {
		Memory m = new Memory(section(key));
		return Point.loadFrom(m);
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

	//FortressGeneratorRune
	public FortressGeneratorRune loadFortressGeneratorRune(String key) {
		Memory m = new Memory(section(key));
		return FortressGeneratorRune.loadFrom(m);
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
		return FortressGeneratorRunePattern.loadFrom(m);
	}

	//GeneratorCore
	public GeneratorCore loadGeneratorCore(String key) {
		Memory m = new Memory(section(key));
		return GeneratorCore.loadFrom(m);
	}

	//GeneratorCoreAnimator
	public GeneratorCoreAnimator loadGenerationAnimator(String key) {
		Memory m = new Memory(section(key));
		return GeneratorCoreAnimator.loadFrom(m);
	}
}
