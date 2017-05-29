package me.newyith.fortress.main;

public class FortressPlugin {
//	public static final boolean releaseBuild = false; //TODO: change this to true for release builds
//
//	private static FortressPlugin instance;
//	private static SaveLoadManager saveLoadManager;
//	private static SandboxSaveLoadManager sandboxSaveLoadManager;
//
//	public static int config_glowstoneDustBurnTimeMs = 1000 * 60 * 60;
//	public static int config_stuckDelayMs = 30 * 1000;
//	public static int config_stuckCancelDistance = 4;
//	public static int config_generationRangeLimit = 128;
//	public static int config_generationBlockLimit = 40000; //roughly 125 empty 8x8x8 rooms (6x6x6 air inside)
//
//	private void loadConfig() {
//		FileConfiguration config = getConfig();
//		if (releaseBuild) {
//			config_glowstoneDustBurnTimeMs = getConfigInt(config, "glowstoneDustBurnTimeMs", config_glowstoneDustBurnTimeMs);
//			config_stuckDelayMs = getConfigInt(config, "stuckDelayMs", config_stuckDelayMs);
//			config_stuckCancelDistance = getConfigInt(config, "stuckCancelDistance", config_stuckCancelDistance);
//			config_generationRangeLimit = getConfigInt(config, "generationRangeLimit", config_generationRangeLimit);
//			config_generationBlockLimit = getConfigInt(config, "generationBlockLimit", config_generationBlockLimit);
//		}
//		saveConfig();
//	}
//	private int getConfigInt(FileConfiguration config, String key, int defaultValue) {
//		if (!config.isInt(key)) {
//			config.set(key, defaultValue);
//		}
//		return config.getInt(key);
//	}
//
//	public void onEnable() {
//		instance = this;
//
//		loadConfig();
//
//		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
//		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
//		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN);
//		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
//
//		saveLoadManager = new SaveLoadManager(this);
//		saveLoadManager.load();
//
//		if (!releaseBuild) {
//			sandboxSaveLoadManager = new SandboxSaveLoadManager(this);
////			sandboxSaveLoadManager.load();
//		}
//
//		EventListener.onEnable(this);
//		TickTimer.onEnable(this);
//		ManualCraftManager.onEnable(this);
//		PearlGlitchFix.onEnable(this);
//	}
//
//	public void onDisable() {
//		saveLoadManager.save();
//		if (!releaseBuild) {
////			sandboxSaveLoadManager.save();
//		}
//
//		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
//		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
//		Log.sendConsole("         >> OFF <<          ", ChatColor.RED);
//		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
//	}
//
//	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//		String commandName = cmd.getName();
//		boolean commandHandled = false;
//
//		// /stuck
//		if (commandName.equalsIgnoreCase("stuck") && sender instanceof Player) {
//			Player player = (Player)sender;
//			Commands.onStuckCommand(player);
//			commandHandled = true;
//		}
//
//		return commandHandled;
//	}
}
