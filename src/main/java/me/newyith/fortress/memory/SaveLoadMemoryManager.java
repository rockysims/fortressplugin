package me.newyith.fortress.memory;

import me.newyith.fortress.generator.FortressGeneratorRunesManager;
import me.newyith.fortress.main.FortressPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveLoadMemoryManager {








//	public static void onEnable(FortressPlugin plugin) {
//		newConfig = new File(getDataFolder(), "newconfig.yml");
//		newConfigz = YamlConfiguration.loadConfiguration(newConfig);
//	}
//
//	public void saveConfig() {
//		try {
//			newConfigz.save(newConfig);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void onDisable(){
//		saveConfig()
//	}


	private static FileConfiguration dataFileConfig;
	private static File dataFile;
	private static ObjectMapper objectMapper = (new ObjectMapper());

	public static void onEnable(FortressPlugin plugin) {
		dataFile = new File(plugin.getDataFolder(), "data.json");
//		dataFileConfig = YamlConfiguration.loadConfiguration(dataFile);
		//dataFileConfig = new FileConfiguration();

		try {
			//if (data.json doesn't exist) make an empty data.json
			if (! dataFile.exists()) {
				(new ObjectMapper()).writeValue(dataFile, new LinkedHashMap<String, Object>());
			}

			MapMemory memory = new MapMemory(objectMapper.readValue(dataFile, Map.class));
			MapMemory m = new MapMemory(memory.section("RunesManager"));

			FortressGeneratorRunesManager.loadFrom(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void onDisable(FortressPlugin plugin) {

		MapMemory memory = new MapMemory();
		MapMemory m = new MapMemory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.saveTo(m);

		//save data.yml
		try {
			//dataFileConfig.save(dataFile);
			objectMapper.writeValue(new FileOutputStream(dataFile), memory.getConfig());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}






	//TODO: reuse this for actual config file in FortressPlugin
//	public static void onEnable_old(FortressPlugin plugin) {
//		Memory memory = new Memory(plugin.getConfig());
//		Memory m = new Memory(memory.section("RunesManager"));
//
//		FortressGeneratorRunesManager.loadFrom(m);
//	}
//
//	public static void onDisable_old(FortressPlugin plugin) {
//		//clear config
//		for(String key : plugin.getConfig().getKeys(false)){
//			plugin.getConfig().set(key, null);
//		}
//
//		Memory memory = new Memory(plugin.getConfig());
//		Memory m = new Memory(memory.section("RunesManager"));
//
//		FortressGeneratorRunesManager.saveTo(m);
//
//		plugin.saveConfig();
//	}
}



//		File folder = Bukkit.getServer().getPluginManager().getPlugin("FortressPlugin").getDataFolder();
//		Debug.msg("data folder path: " + folder.getAbsolutePath());
//		File file = new File(folder, "manual.txt");
//		if (file.exists()) {
//
//		}

//		configFile = new File(, "config.yml");
//		if (configFile.exists()) {
//			myConfig = new YamlConfiguration();
//			try {
//				myConfig.load(configFile);
//			} catch (FileNotFoundException ex) {
//				// TODO: Log exception
//			} catch (IOException ex) {
//				// TODO: Log exception
//			} catch (InvalidConfigurationException ex) {
//				// TODO: Log exception
//			}
//			loaded = true;
//		}





//		PrintWriter writer = null;
//		try {
//			writer = new PrintWriter("test.txt", "UTF-8");
//			writer.println("tested");
//			writer.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//
//
//		try {
//			BufferedReader br = new BufferedReader(new FileReader("test.txt"));
//			try {
//				StringBuilder sb = new StringBuilder();
//				String line = br.readLine();
//
//				while (line != null) {
//					sb.append(line);
//					sb.append(System.lineSeparator());
//					line = br.readLine();
//				}
//				String everything = sb.toString();
//				Debug.msg("everything: " + everything);
//			} finally {
//				br.close();
//			}
//		} catch (java.io.FileNotFoundException e) {
//			Debug.error("caught FileNotFoundException: " + e.getMessage());
//		} catch (java.io.IOException e) {
//			e.printStackTrace();
//		}