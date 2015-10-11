package me.newyith.fortress.memory;

import com.google.common.base.Splitter;
import me.newyith.fortress.generator.FortressGeneratorRune;
import me.newyith.fortress.generator.FortressGeneratorRunePattern;
import me.newyith.fortress.generator.GeneratorCore;
import me.newyith.fortress.generator.GeneratorCoreAnimator;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public abstract class AbstractMemory<ConfigType> {
	protected ConfigType config;

	public AbstractMemory(ConfigType config) {
		this.config = config;
	}

	public abstract ConfigType section(String sectionName);

	public ConfigType getConfig() {
		return config;
	}

	// --- PRIMITIVE SAVE/LOAD ---

	//boolean
	abstract public void save(String key, boolean value);
	abstract public boolean loadBoolean(String key);

	//int
	abstract public void save(String key, int value);
	abstract public int loadInt(String key);

	//long
	abstract public void save(String key, long value);
	abstract public long loadLong(String key);

	//String
	abstract public void save(String key, String value);
	abstract public String loadString(String key);
	
	abstract AbstractMemory<ConfigType> newMemory(ConfigType config);

	// --- SAVE/LOAD COMPACT ---

	//Set<Point> (compact)
	public void savePointSetCompact(String key, Set<Point> set) {
		AbstractMemory m = newMemory(section(key));
		Point[] ary = set.toArray(new Point[set.size()]);
		ArrayList<Point> list = new ArrayList<>(Arrays.asList(ary));
		m.savePointListCompact(key, list);
	}
	public Set<Point> loadPointSetCompact(String key) {
		AbstractMemory m = newMemory(section(key));

		List<Point> list = m.loadPointListCompact(key);
		Set<Point> set = new HashSet<>(list);

		return set;
	}

	//List<Point> (compact)
	public void savePointListCompact(String key, List<Point> list) {
		AbstractMemory m = newMemory(section(key));

		StringBuilder blob = new StringBuilder();
		if (list.size() > 0) {
			m.save("world", list.get(0).world.getName());

			for (Point p : list) {
				StringBuilder pointBlob = new StringBuilder();
				pointBlob.append((int)p.x);
				pointBlob.append(",");
				pointBlob.append((int) p.y);
				pointBlob.append(",");
				pointBlob.append((int) p.z);
				pointBlob.append("~");
				blob.append(pointBlob);
			}
			blob.setLength(Math.max(blob.length() - 1, 0)); //remove last "~"
		}
		m.save("blob", blob.toString());
	}
	public List<Point> loadPointListCompact(String key) {
		AbstractMemory m = newMemory(section(key));

		ArrayList<Point> list = new ArrayList<>();
		String blob = m.loadString("blob");
		if (blob.length() > 0) {
			World world = Bukkit.getWorld(m.loadString("world"));
			for (String pointBlob : Splitter.on("~").split(blob)) {
				List<String> data = Splitter.on(",").splitToList(pointBlob);
				int x = Integer.valueOf(data.get(0));
				int y = Integer.valueOf(data.get(1));
				int z = Integer.valueOf(data.get(2));
				list.add(new Point(world, x, y, z));
			}
		}

		return list;
	}

	//List<List<Point>> (compact)
	public void saveLayersCompact(String key, List<List<Point>> layers) {
		AbstractMemory m = newMemory(section(key));

		int i = 0;
		for (List<Point> layer : layers) {
			m.savePointListCompact(Integer.toString(i++), layer);
		}
		m.save("count", i);
	}
	public List<List<Point>> loadLayersCompact(String key) {
		AbstractMemory m = newMemory(section(key));

		List<List<Point>> layers = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			layers.add(m.loadPointListCompact(Integer.toString(i)));
		}

		return layers;
	}

	//HashMap<Point, Material> (compact)
	public void savePointMaterialMapCompact(String key, HashMap<Point, Material> map) {
		AbstractMemory m = newMemory(section(key));

		StringBuilder blob = new StringBuilder();
		if (map.size() > 0) {
			Point firstKey = map.keySet().iterator().next();
			m.save("world", firstKey.world.getName());

			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				Point p = (Point) pair.getKey();
				Material mat = (Material) pair.getValue();

				StringBuilder pairBlob = new StringBuilder();
				pairBlob.append((int) p.x);
				pairBlob.append(",");
				pairBlob.append((int) p.y);
				pairBlob.append(",");
				pairBlob.append((int) p.z);
				pairBlob.append(",");
				pairBlob.append(mat.ordinal());
				blob.append(pairBlob + "~");
			}
			blob.setLength(Math.max(blob.length() - 1, 0)); //remove last "~"
		}
		m.save("blob", blob.toString());
	}
	public HashMap<Point, Material> loadPointMaterialMapCompact(String key) {
		AbstractMemory m = newMemory(section(key));

		HashMap<Point, Material> map = new HashMap<>();
		String blob = m.loadString("blob");
		if (blob.length() > 0) {
			World world = Bukkit.getWorld(m.loadString("world"));
			for (String pairBlob : Splitter.on("~").splitToList(blob)) {
				List<String> data = Splitter.on(",").splitToList(pairBlob);
				int x = Integer.valueOf(data.get(0));
				int y = Integer.valueOf(data.get(1));
				int z = Integer.valueOf(data.get(2));
				Material mat = Material.values()[Integer.valueOf(data.get(3))];
				Point p = new Point(world, x, y, z);
				map.put(p, mat);
			}
		}

		return map;
	}

	// --- SAVE ---

	//Memorable
	public void save(String key, Memorable value) {
		AbstractMemory m = newMemory(section(key));
		value.saveTo(m);
	}

	//List<Memorable>
	public void save(String key, List list) {
		AbstractMemory m = newMemory(section(key));
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
		AbstractMemory m = newMemory(section(key));
		Point[] ary = set.toArray(new Point[set.size()]);
		ArrayList<Point> list = new ArrayList<>(Arrays.asList(ary));
		m.save(key, list);
	}

	//HashMap<Point, Material>
	public void save(String key, HashMap<Point, Material> map) {
		AbstractMemory m = newMemory(section(key));

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
		AbstractMemory m = newMemory(section(key));
		return Point.loadFrom(m);
	}

	//ArrayList<Point>
	public ArrayList<Point> loadPointList(String key) {
		AbstractMemory m = newMemory(section(key));

		ArrayList<Point> list = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			list.add(m.loadPoint(Integer.toString(i)));
		}

		return list;
	}

	//List<List<Point>>
	public List<List<Point>> loadLayers(String key) {
		AbstractMemory m = newMemory(section(key));

		List<List<Point>> layers = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			layers.add(m.loadPointList(Integer.toString(i)));
		}

		return layers;
	}

	//Set<Point>
	public Set<Point> loadPointSet(String key) {
		AbstractMemory m = newMemory(section(key));

		List<Point> list = m.loadPointList(key);
		Set<Point> set = new HashSet<>(list);

		return set;
	}

	//HashMap<Point, Material>
	public HashMap<Point, Material> loadPointMaterialMap(String key) {
		AbstractMemory m = newMemory(section(key));

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
		AbstractMemory m = newMemory(section(key));
		return FortressGeneratorRune.loadFrom(m);
	}

	//ArrayList<FortressGeneratorRune>
	public ArrayList<FortressGeneratorRune> loadFortressGeneratorRunes(String key) {
		AbstractMemory m = newMemory(section(key));

		ArrayList<FortressGeneratorRune> list = new ArrayList<>();
		int count = m.loadInt("count");
		for (int i = 0; i < count; i++) {
			list.add(m.loadFortressGeneratorRune(Integer.toString(i)));
		}

		return list;
	}

	//FortressGeneratorRunePattern
	public FortressGeneratorRunePattern loadFortressGeneratorRunePattern(String key) {
		AbstractMemory m = newMemory(section(key));
		return FortressGeneratorRunePattern.loadFrom(m);
	}

	//GeneratorCore
	public GeneratorCore loadGeneratorCore(String key) {
		AbstractMemory m = newMemory(section(key));
		return GeneratorCore.loadFrom(m);
	}

	//GeneratorCoreAnimator
	public GeneratorCoreAnimator loadGenerationAnimator(String key) {
		AbstractMemory m = newMemory(section(key));
		return GeneratorCoreAnimator.loadFrom(m);
	}
}
