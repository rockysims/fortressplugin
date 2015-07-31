package me.newyith.memory;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;

public class ConfigManager {
	private FortressPlugin plugin;

	public ConfigManager(FortressPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Memory memory = new Memory(plugin.getConfig());
		Memory m = new Memory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.loadFrom(m);
	}

	public void onDisable() {
		Memory memory = new Memory(plugin.getConfig());
		Memory m = new Memory(memory.section("RunesManager"));

		FortressGeneratorRunesManager.saveTo(m);

		plugin.saveConfig();
	}
}
