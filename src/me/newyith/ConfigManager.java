package me.newyith;

public class ConfigManager {
	private FortressPlugin plugin;

	public ConfigManager(FortressPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Memory memory = new Memory(plugin.getConfig());

		Memory m = new Memory(memory.section("RunesManager"));
		FortressGeneratorRunesManager.saveTo(m);
	}

	public void onDisable() {
		Memory memory = new Memory(plugin.getConfig());

		Memory m = new Memory(memory.section("RunesManager"));
		FortressGeneratorRunesManager.loadFrom(m);

		plugin.saveConfig();
	}
}
