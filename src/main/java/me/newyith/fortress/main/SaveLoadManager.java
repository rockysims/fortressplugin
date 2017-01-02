package me.newyith.fortress.main;

import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.protection.ProtectionManager;
import me.newyith.fortress.util.BatchDataStore;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SaveLoadManager implements Listener {
	private final int saveWithWorldsCooldownMs = 5000;
	private long lastSaveTimestamp = 0;
	private File dataFile = new File(FortressPlugin.getInstance().getDataFolder(), "data.json");
	private static File bedrockSafetyFile = new File(FortressPlugin.getInstance().getDataFolder(), "bedrockSafety.json");
	private static final ObjectMapper mapper = new ObjectMapper();

	public SaveLoadManager(FortressPlugin plugin) {
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onWorldSave(WorldSaveEvent e) {
		long now = new Date().getTime();
		long elapsed = now - lastSaveTimestamp;
		Debug.msg("elapsed: " + elapsed);
		if (elapsed > saveWithWorldsCooldownMs) {
			Debug.msg("save is cooled");
			lastSaveTimestamp = now;

			long startSaveMs = System.currentTimeMillis();
			save();
			long endSaveMs = System.currentTimeMillis();
			long saveMs = endSaveMs - startSaveMs;
			Log.success("Saved " + FortressesManager.getRuneCountForAllWorlds() + " rune(s) in " + ((saveMs / 10) / 100F) + " seconds.");
		} else {
			Debug.msg("save is still cooling down");
		}
	}

	private void saveToMap(Map<String, Object> data) {
		data.put("BatchDataStore", BatchDataStore.getInstance());
		data.put("TimedBedrockManager", TimedBedrockManager.getInstance());
		data.put("ProtectionManager", ProtectionManager.getInstance());
		data.put("FortressesManager", FortressesManager.getInstance());
		data.put("BedrockManager", BedrockManager.getInstance());
	}

	private void loadFromMap(Map<String, Object> data) {
		Object obj;

		boolean useTimers = true;

		if (useTimers) Debug.start("loadFromMap()");

		if (useTimers) Debug.start("loadBatchDataStore");
		//load BatchDataStore
		obj = data.get("BatchDataStore");
		if (obj == null) {
			BatchDataStore.setInstance(new BatchDataStore());
		} else {
			BatchDataStore batchDataStore = mapper.convertValue(obj, BatchDataStore.class);
			BatchDataStore.setInstance(batchDataStore);
		}
		if (useTimers) Debug.end("loadBatchDataStore");

		if (useTimers) Debug.start("loadTimedBedrockManager");
		//load TimedBedrockManager
		obj = data.get("TimedBedrockManager");
		if (obj == null) {
			TimedBedrockManager.setInstance(new TimedBedrockManager());
		} else {
			TimedBedrockManager timedBedrockManager = mapper.convertValue(obj, TimedBedrockManager.class);
			TimedBedrockManager.setInstance(timedBedrockManager);
		}
		if (useTimers) Debug.end("loadTimedBedrockManager");

		if (useTimers) Debug.start("loadProtectionManager");
		//load ProtectionManager
		obj = data.get("ProtectionManager");
		if (obj == null) {
			ProtectionManager.setInstance(new ProtectionManager());
		} else {
//			Debug.msg("load obj (PM) type: " + obj.getClass().getName());
			ProtectionManager protectionManager = mapper.convertValue(obj, ProtectionManager.class);
			ProtectionManager.setInstance(protectionManager);
		}
		if (useTimers) Debug.end("loadProtectionManager");

		if (useTimers) Debug.start("loadFortressesManager");
		//load FortressesManager
		obj = data.get("FortressesManager");
		if (obj == null) {
			FortressesManager.setInstance(new FortressesManager());
		} else {
//			Debug.msg("load obj (FM) type: " + obj.getClass().getName());
			if (useTimers) Debug.start("loadFortressesManager:mapper");
			FortressesManager fortressesManager = mapper.convertValue(obj, FortressesManager.class);
			if (useTimers) Debug.end("loadFortressesManager:mapper");
			if (useTimers) Debug.start("loadFortressesManager:setInstance");
			FortressesManager.setInstance(fortressesManager);
			if (useTimers) Debug.end("loadFortressesManager:setInstance");
			if (useTimers) Debug.start("loadFortressesManager:secondStageLoad");
			FortressesManager.secondStageLoad();
			if (useTimers) Debug.end("loadFortressesManager:secondStageLoad");
		}
		if (useTimers) Debug.end("loadFortressesManager");

		if (useTimers) Debug.start("loadBedrockManager");
		//load BedrockManager
		obj = data.get("BedrockManager");
		if (obj == null) {
			BedrockManager.setInstance(new BedrockManager());
		} else {
//			Debug.msg("load obj (BM) type: " + obj.getClass().getName());
			if (useTimers) Debug.start("loadBedrockManager:mapper");
			BedrockManager bedrockManager = mapper.convertValue(obj, BedrockManager.class);
			if (useTimers) Debug.end("loadBedrockManager:mapper");

			/* debug code
			for (int i = 0; i < 5; i++) {
				if (useTimers) Debug.start("loadBedrockManager:mapper" + i);
//				ObjectMapper curMapper = new ObjectMapper();
//				curMapper.convertValue(obj, BedrockManager.class);
				mapper.convertValue(obj, BedrockManager.class);
				if (useTimers) Debug.end("loadBedrockManager:mapper" + i);
			}
			//*/

			BedrockManager.setInstance(bedrockManager);
		}
		if (useTimers) Debug.end("loadBedrockManager");

		if (useTimers) Debug.end("loadFromMap()");
	}

	public void save() {
		//TODO: consider adding loadFuture and not allowing save until it resolves
//		boolean loaded = loadFuture.getNow(false);
//		if (!loaded) Log.success("Save pending...");
//
//		loadFuture.thenAccept(param1 -> {
//			long startSaveMs = System.currentTimeMillis();
//
//			saveLoadManager.save();
//
//			long endSaveMs = System.currentTimeMillis();
//			long saveMs = endSaveMs - startSaveMs;
//			Log.success("Saved " + FortressesManager.getRuneCountForAllWorlds() + " rune(s) in " + ((saveMs / 10) / 100F) + " seconds.");
//		});

		Debug.start("save");
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			saveToBuffer(buffer);
			//write buffer to file
			FileOutputStream fos = new FileOutputStream(dataFile);
			fos.write(buffer.toByteArray(), 0, buffer.size());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Debug.end("save");
	}

	public CompletableFuture<Boolean> loadAsync() {
		long startLoadMs = System.currentTimeMillis();
		Log.success("Loading... (async)");
		CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
			load();
			long loadMs = System.currentTimeMillis() - startLoadMs;
			Log.success("Loaded " + FortressesManager.getRuneCountForAllWorlds() + " rune(s) in " + ((loadMs / 10) / 100F) + " seconds.");
			return true;
		});

		return future;
	}

	private void load() {
		Debug.start("load");
		try {
			//if (data.json doesn't exist) make an empty data.json
			if (! dataFile.exists()) {
				mapper.writeValue(dataFile, new LinkedHashMap<String, Object>());
			}

			Debug.start("load:mapper");
			Map<String, Object> data = mapper.readValue(dataFile, Map.class);
			Debug.end("load:mapper");
			loadFromMap(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Debug.end("load");

		Debug.start("onAfterLoad");
		onAfterLoad();
		Debug.end("onAfterLoad");

		//if (!releaseBuild) do mock save so that /reload can still save after recompiling plugin
		//	mock save loads classes needed for saving (new classes can't be loaded after I rebuild jar)
		if (!FortressPlugin.releaseBuild) {
			//save on another thread so that:
			//- it doesn't get counted as part of load time
			//- minecraft can continue ticking while mock save happens
			CompletableFuture.supplyAsync(() -> {
				try {
					saveToBuffer(new ByteArrayOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			});
		}
	}

	private void saveToBuffer(OutputStream stream) throws IOException {
		Debug.start("saveToBuffer()");
		Map<String, Object> data = new HashMap<>();
		saveToMap(data);
		Debug.start("saveToBuffer()map");
		mapper.writeValue(stream, data);
		Debug.end("saveToBuffer()map");
		Debug.end("saveToBuffer()");
	}

	// Bedrock Safety //

	public static void saveBedrockSafety() {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			Map<String, Object> data = new HashMap<>();
			data.put("BedrockSafety", BedrockSafety.getInstance());
			Debug.msg("Saved BedrockSafety"); //LATER: delete this line?

			mapper.writeValue(buffer, data);

			//write buffer to file
			FileOutputStream fos = new FileOutputStream(bedrockSafetyFile);
			fos.write(buffer.toByteArray(), 0, buffer.size());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void onAfterLoad() {
		try {
			//if (bedrockSafety.json doesn't exist) make an empty bedrockSafety.json
			if (! bedrockSafetyFile.exists()) {
				mapper.writeValue(bedrockSafetyFile, new LinkedHashMap<String, Object>());
			}

			Map<String, Object> data = mapper.readValue(bedrockSafetyFile, Map.class);

			//load BedrockSafety
			Object obj = data.get("BedrockSafety");
			if (obj == null) {
				BedrockSafety.setInstance(new BedrockSafety());
			} else {
//				Debug.msg("load obj (BS) type: " + obj.getClass().getName());
				BedrockSafety bedrockSafety = mapper.convertValue(obj, BedrockSafety.class);
				BedrockSafety.setInstance(bedrockSafety);
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
				BedrockSafety.safetySync();
			}, 0); //ticks (50 ms per tick)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
