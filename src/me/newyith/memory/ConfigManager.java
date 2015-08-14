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
		Memory memory = new Memory(plugin.getConfig());
		Memory m = new Memory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.saveTo(m);

		plugin.saveConfig();
	}
}