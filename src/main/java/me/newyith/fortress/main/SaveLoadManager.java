package me.newyith.fortress.main;

import me.newyith.fortress.util.Debug;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveLoadManager {
	private File dataFile;
	private ObjectMapper mapper = new ObjectMapper();

	public SaveLoadManager(FortressPlugin plugin) {
		dataFile = new File(plugin.getDataFolder(), "data.json");
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
	}

	private void saveToMap(Map<String, Object> data) {
//		data.put("FortressesManager", FortressesManager.getInstance());


		//save FortressesManager
		data.put("FortressesManager", FortressesManager.getInstance());
		Debug.msg("Saved " + FortressesManager.getRuneCount() + " rune(s).");
	}

	private void loadFromMap(Map<String, Object> data) {
		//*

		//load FortressesManager
		Object obj = data.get("FortressesManager");
		if (obj == null) {
			FortressesManager.setInstance(new FortressesManager());
		} else {
			Debug.msg("load obj (FM) type: " + obj.getClass().getName());
			FortressesManager fortressesManager = mapper.convertValue(obj, FortressesManager.class);
			FortressesManager.setInstance(fortressesManager);
		}
		Debug.msg("Loaded " + FortressesManager.getRuneCount() + " rune(s).");

//		Object obj = data.get("FortressesManager");
//		if (obj == null) {
//			FortressesManager.setInstance(new FortressesManager());
//		} else if (obj instanceof FortressesManager) {
//			FortressesManager.setInstance((FortressesManager) obj);
//		} else {
//			Debug.error("Failed to load FortressesManager because obj is not instanceof FortressesManager.Model");
//		}

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

		if (!FortressPlugin.releaseBuild) {
			//do mock save so needed classes are loaded (new classes can't be loaded after I rebuild jar)
			try {
				Debug.msg("dummy saving");
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
}
