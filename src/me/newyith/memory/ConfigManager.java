package me.newyith.memory;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;

public class ConfigManager {
	public static void onEnable(FortressPlugin plugin) {
		Memory memory = new Memory(plugin.getConfig());
		Memory m = new Memory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.loadFrom(m);
	}

	public static void onDisable(FortressPlugin plugin) {
		//clear config
		for(String key : plugin.getConfig().getKeys(false)){
			plugin.getConfig().set(key, null);
		}

		Memory memory = new Memory(plugin.getConfig());
		Memory m = new Memory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.saveTo(m);

		plugin.saveConfig();
	}
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