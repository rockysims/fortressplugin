package me.newyith;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

public class ConfigManager {
	private FortressPlugin plugin;

	public ConfigManager(FortressPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		FortressGeneratorRunesManager.loadFromConfig(subsection("RunesManager"));
	}

	public void onDisable() {
		FortressGeneratorRunesManager.saveToConfig(subsection("RunesManager"));
		plugin.saveConfig();
	}

	private ConfigurationSection subsection(String sectionName) {
		return subsection(plugin.getConfig(), sectionName);
	}

	public static ConfigurationSection subsection(ConfigurationSection section, String subsectionName) {
		ConfigurationSection subsection = section.getConfigurationSection(subsectionName);
		if (subsection == null) {
			subsection = section.createSection(subsectionName);
		}
		return subsection;
	}

	//boolean
	public static void save(ConfigurationSection config, String key, boolean value) {
		config.set(key, value);
	}
	public static boolean loadBoolean(ConfigurationSection config, String key) {
		return config.getBoolean(key);
	}

	//Point
	public static void save(ConfigurationSection config, String key, Point value) {

	}
	public static Point loadPoint(ConfigurationSection config, String key) {
		return null;
	}

	//ArrayList<Point>
	public static void savePoints(ConfigurationSection config, String key, ArrayList<Point> value) {

	}
	public static ArrayList<Point> loadPoints(ConfigurationSection config, String key) {
		return null;
	}

	//FortressGeneratorRunePattern
	public static void save(ConfigurationSection config, String key, FortressGeneratorRunePattern value) {

	}
	public static FortressGeneratorRunePattern loadFortressGeneratorRunePattern(ConfigurationSection config, String key) {
		return null;
	}

	//ArrayList<FortressGeneratorRune>
	public static void saveFortressGeneratorRunes(ConfigurationSection config, String key, ArrayList<FortressGeneratorRune> value) {

	}
	public static ArrayList<FortressGeneratorRune> loadFortressGeneratorRunes(ConfigurationSection config, String key) {
		return null;
	}







//		ConfigurationSection patternConfig = ConfigManager.subsection(config, "pattern");
//		if (pattern != null) {
//			patternConfig.set("!null", true);
//			pattern.saveToConfig(patternConfig);
//		}

//		ConfigurationSection patternConfig = ConfigManager.subsection(config, "pattern");
//		if (patternConfig.getBoolean("!null")) {
//			pattern = new FortressGeneratorRunePattern(null);
//			pattern.loadFromConfig(patternConfig);
//		}



//		Object patternObj = ConfigManager.load(config, "pattern", pattern);
//		if (patternObj instanceof FortressGeneratorRunePattern) {
//			pattern = (FortressGeneratorRunePattern)patternObj;
//		}

//		for (int i = 0; i < runeInstances.size(); i++) {
//			runeInstances.get(i).saveToConfig(ConfigManager.subsection(config, "rune" + i));
//		}
//		config.set("runeInstances.size()", runeInstances.size());

//		Object runeInstancesObj = ConfigManager.load(config, "runeInstances");
//		if (runeInstancesObj != null) {
//			runeInstances = (FortressGeneratorRunePattern)runeInstancesObj;
//		}


//		int size = config.getInt("runeInstances.size()");
//		for (int i = 0; i < size; i++) {
//			runeInstances.get(i).loadFromConfig(ConfigManager.subsection(config, "rune" + i));
//		}





}
