package me.newyith.fortress.sandbox.jackson;

import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.util.Debug;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SandboxSaveLoadManager {
	private File dataFile;
	private ObjectMapper mapper = new ObjectMapper();

	public SandboxSaveLoadManager(FortressPlugin plugin) {
		dataFile = new File(plugin.getDataFolder(), "sandbox.json");
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
	}

	private void saveToMap(Map<String, Object> data) {
		SandboxThingToSave thing = SandboxThingToSave.getInstance();
//		thing.setDatum(String.valueOf(new Random().nextInt()));
		data.put("thing", thing);
		thing.print("saved thing");
	}

	private void loadFromMap(Map<String, Object> data) {
		Object obj = data.get("thing");
		if (obj == null) {
			SandboxThingToSave thing = new SandboxThingToSave("loadObjNullDatum");
			SandboxThingToSave.setInstance(thing);
		} else {
			Debug.msg("loadFromMap(). obj class name: " + obj.getClass().getName());
			SandboxThingToSave thing = mapper.convertValue(obj, SandboxThingToSave.class);
			SandboxThingToSave.setInstance(thing);
			thing.print("loaded thing");
		}
	}

	public void save() {
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
	}

	public void load() {
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
}
