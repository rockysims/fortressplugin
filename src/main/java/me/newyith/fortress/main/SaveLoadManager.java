package me.newyith.fortress.main;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.protection.ProtectionManager;
import me.newyith.fortress.util.BatchDataStore;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Log;
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
	private final int saveWithWorldsCooldownMs = 500;
	private long lastSaveTimestamp = 0;
	private final File dataFile = new File(FortressPlugin.getInstance().getDataFolder(), "data.json");
	private static final File bedrockSafetyFile = new File(FortressPlugin.getInstance().getDataFolder(), "bedrockSafety.json");
	private static final ObjectMapper mapper = new ObjectMapper();

	public SaveLoadManager(FortressPlugin plugin) {
		mapper.registerModule(new GuavaModule());
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
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
			save();
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

		Log.progress("Loading ==-----");
		//load BatchDataStore
		obj = data.get("BatchDataStore");
		if (obj == null) {
			BatchDataStore.setInstance(new BatchDataStore());
		} else {
			BatchDataStore batchDataStore = mapper.convertValue(obj, BatchDataStore.class);
			BatchDataStore.setInstance(batchDataStore);
		}

		Log.progress("Loading ===----");
		//load TimedBedrockManager
		obj = data.get("TimedBedrockManager");
		if (obj == null) {
			TimedBedrockManager.setInstance(new TimedBedrockManager());
		} else {
			TimedBedrockManager timedBedrockManager = mapper.convertValue(obj, TimedBedrockManager.class);
			TimedBedrockManager.setInstance(timedBedrockManager);
		}

		Log.progress("Loading ====---");
		//load ProtectionManager
		obj = data.get("ProtectionManager");
		if (obj == null) {
			ProtectionManager.setInstance(new ProtectionManager());
		} else {
//			Debug.msg("load obj (PM) type: " + obj.getClass().getName());
			ProtectionManager protectionManager = mapper.convertValue(obj, ProtectionManager.class);
			ProtectionManager.setInstance(protectionManager);
		}

		Log.progress("Loading =====--");
		//load FortressesManager
		obj = data.get("FortressesManager");
		if (obj == null) {
			FortressesManager.setInstance(new FortressesManager());
		} else {
//			Debug.msg("load obj (FM) type: " + obj.getClass().getName());
			FortressesManager fortressesManager = mapper.convertValue(obj, FortressesManager.class);
			FortressesManager.setInstance(fortressesManager);
		}

		Log.progress("Loading ======-");
		//load BedrockManager
		obj = data.get("BedrockManager");
		if (obj == null) {
			BedrockManager.setInstance(new BedrockManager());
		} else {
//			Debug.msg("load obj (BM) type: " + obj.getClass().getName());
			BedrockManager bedrockManager = mapper.convertValue(obj, BedrockManager.class);
			BedrockManager.setInstance(bedrockManager);
		}

		Log.progress("Loading =======");
		FortressesManager.secondStageLoad();
	}

	public void save() {
//		Debug.start("save");
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
//		Debug.end("save");

		Log.success("Saved " + FortressesManager.getRuneCountForAllWorlds() + " rune(s).");
	}

	public void load() {
//		Debug.start("load");
		try {
			//if (data.json doesn't exist) make an empty data.json
			if (! dataFile.exists()) {
				mapper.writeValue(dataFile, new LinkedHashMap<String, Object>());
			}

			Log.progress("Loading =------");
			Map<String, Object> data = mapper.readValue(dataFile, Map.class);
			loadFromMap(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		Debug.end("load");

		Log.success("Loaded " + FortressesManager.getRuneCountForAllWorlds() + " rune(s).");

		onAfterLoad();

		if (!FortressPlugin.releaseBuild) {
			//do mock save so needed classes are loaded (new classes can't be loaded after I rebuild jar)
			try {
				saveToBuffer(new ByteArrayOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveToBuffer(OutputStream stream) throws IOException {
		Map<String, Object> data = new HashMap<>();
		saveToMap(data);
		mapper.writeValue(stream, data);
	}

	// Bedrock Safety //

	public static CompletableFuture<Void> saveBedrockSafety() {
		final BedrockSafety bedrockSafetyInstance = BedrockSafety.getInstance();
		return CompletableFuture.supplyAsync(() -> {
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				//synchronized to prevent changes to BedrockSafety during writeValue(buffer, data)
				synchronized (BedrockSafety.mutex) {
					Map<String, Object> data = new HashMap<>();
					data.put("BedrockSafety", bedrockSafetyInstance);
					mapper.writer().writeValue(buffer, data);
				}

				synchronized (bedrockSafetyFile) {
					//write buffer to file
					FileOutputStream fos = new FileOutputStream(bedrockSafetyFile);
					fos.write(buffer.toByteArray(), 0, buffer.size());
					fos.close();
				}

				Debug.msg("Saved BedrockSafety"); //LATER: delete this line?
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		});
	}

	private void onAfterLoad() {
		try {
			Map<String, Object> data;
			synchronized (bedrockSafetyFile) {
				//if (bedrockSafety.json doesn't exist) make an empty bedrockSafety.json
				if (! bedrockSafetyFile.exists()) {
					mapper.writeValue(bedrockSafetyFile, new LinkedHashMap<String, Object>());
				}

				data = mapper.readValue(bedrockSafetyFile, Map.class);
			}

			//load BedrockSafety
			Object obj = data.get("BedrockSafety");
			if (obj == null) {
				BedrockSafety.setInstance(new BedrockSafety());
			} else {
//				Debug.msg("load obj (BS) type: " + obj.getClass().getName());
				BedrockSafety bedrockSafety = mapper.convertValue(obj, BedrockSafety.class);
				BedrockSafety.setInstance(bedrockSafety);
			}

			BedrockSafety.safetySync();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
