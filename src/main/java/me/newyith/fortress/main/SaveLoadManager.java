package me.newyith.fortress.main;

import me.newyith.fortress.util.Debug;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveLoadManager {
	private File dataFile;
	private ObjectMapper objectMapper = (new ObjectMapper());

	public SaveLoadManager(FortressPlugin plugin) {
		dataFile = new File(plugin.getDataFolder(), "data.json");
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

			Map<String, Object> data = objectMapper.readValue(dataFile, Map.class);

			//load FortressesManager
			Object obj = data.get("FortressesManager");
			if (obj instanceof FortressesManager.Model) {
				FortressesManager.setModel((FortressesManager.Model) obj);
			} else {
				Debug.error("Failed to load FortressesManager because obj is not instanceof FortressesManager.Model");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Debug.end("load");

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

		//save FortressesManager
		data.put("FortressesManager", FortressesManager.getModel());

		objectMapper.writeValue(stream, data);
	}
}
