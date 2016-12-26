package me.newyith.fortress.main;

import me.newyith.fortress.bedrock.BedrockManagerNew;
import me.newyith.fortress.bedrock.timed.TimedBedrockManagerNew;
import me.newyith.fortress.protection.ProtectionManager;
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

public class SaveLoadManager implements Listener {
	private final int saveWithWorldsCooldownMs = 500;
	private long lastSaveTimestamp = 0;
	private File dataFile = new File(FortressPlugin.getInstance().getDataFolder(), "data.json");
	private static File bedrockSafetyFile = new File(FortressPlugin.getInstance().getDataFolder(), "bedrockSafety.json");
	private static ObjectMapper mapper = new ObjectMapper();

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
			save();
		} else {
			Debug.msg("save is still cooling down");
		}
	}

	private void saveToMap(Map<String, Object> data) {
		data.put("TimedBedrockManager", TimedBedrockManagerNew.getInstance());
		data.put("ProtectionManager", ProtectionManager.getInstance());
		data.put("FortressesManager", FortressesManager.getInstance());
		data.put("BedrockManager", BedrockManagerNew.getInstance());
	}

	private void loadFromMap(Map<String, Object> data) {
		Object obj;

		//load TimedBedrockManager
		obj = data.get("TimedBedrockManager");
		if (obj == null) {
			TimedBedrockManagerNew.setInstance(new TimedBedrockManagerNew());
		} else {
			TimedBedrockManagerNew timedBedrockManager = mapper.convertValue(obj, TimedBedrockManagerNew.class);
			TimedBedrockManagerNew.setInstance(timedBedrockManager);
		}

		//load ProtectionManager
		obj = data.get("ProtectionManager");
		if (obj == null) {
			ProtectionManager.setInstance(new ProtectionManager());
		} else {
//			Debug.msg("load obj (PM) type: " + obj.getClass().getName());
			ProtectionManager protectionManager = mapper.convertValue(obj, ProtectionManager.class);
			ProtectionManager.setInstance(protectionManager);
		}

		//load FortressesManager
		obj = data.get("FortressesManager");
		if (obj == null) {
			FortressesManager.setInstance(new FortressesManager());
		} else {
//			Debug.msg("load obj (FM) type: " + obj.getClass().getName());
			FortressesManager fortressesManager = mapper.convertValue(obj, FortressesManager.class);
			FortressesManager.setInstance(fortressesManager);
			FortressesManager.secondStageLoad();
		}

		//load BedrockManager
		obj = data.get("BedrockManager");
		if (obj == null) {
			BedrockManagerNew.setInstance(new BedrockManagerNew());
		} else {
//			Debug.msg("load obj (BM) type: " + obj.getClass().getName());
			BedrockManagerNew bedrockManager = mapper.convertValue(obj, BedrockManagerNew.class);
			BedrockManagerNew.setInstance(bedrockManager);
		}
	}

	public void save() {
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

		Log.success("Saved " + FortressesManager.getRuneCountForAllWorlds() + " rune(s).");
	}

	public void load() {
		Debug.start("load");
		try {
			//if (data.json doesn't exist) make an empty data.json
			if (! dataFile.exists()) {
				(new ObjectMapper()).writeValue(dataFile, new LinkedHashMap<String, Object>());
			}

			Map<String, Object> data = mapper.readValue(dataFile, Map.class);
			loadFromMap(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Debug.end("load");

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
				(new ObjectMapper()).writeValue(bedrockSafetyFile, new LinkedHashMap<String, Object>());
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

			BedrockSafety.safetySync();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
