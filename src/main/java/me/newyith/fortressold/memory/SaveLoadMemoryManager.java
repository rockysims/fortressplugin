package me.newyith.fortressold.memory;

import me.newyith.fortressold.generator.FortressGeneratorRunesManager;
import me.newyith.fortressold.main.FortressPlugin;
import me.newyith.fortressold.util.Debug;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveLoadMemoryManager {
	private static File dataFile;
	private static ObjectMapper objectMapper = (new ObjectMapper());

	public static void onEnable(FortressPlugin plugin) {
		Debug.start("load");

		dataFile = new File(plugin.getDataFolder(), "data.json");

		try {
			//if (data.json doesn't exist) make an empty data.json
			if (! dataFile.exists()) {
				(new ObjectMapper()).writeValue(dataFile, new LinkedHashMap<String, Object>());
			}

			Debug.start("loadPrep"); //slowish but scales well
			Map<String, Object> memMap = objectMapper.readValue(dataFile, Map.class);
			Debug.end("loadPrep");
			MapMemory memory = new MapMemory(memMap);
			MapMemory m = new MapMemory(memory.section("RunesManager"));

			Debug.start("loadFrom"); //slow and scales poorly
			FortressGeneratorRunesManager.loadFrom(m);
			Debug.end("loadFrom");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Debug.end("load");

		if (!FortressPlugin.releaseBuild) {
			//do mock save so needed classes are loaded (new classes can't be loaded after I rebuild jar)
			try {
				writeDataToBuffer(new ByteArrayOutputStream());
			} catch (IOException e) {}
		}
	}

	private static void writeDataToBuffer(OutputStream stream) throws IOException {
		MapMemory memory = new MapMemory();
		MapMemory m = new MapMemory(memory.section("RunesManager"));
		Debug.start("saving runes manager");
		FortressGeneratorRunesManager.saveTo(m);
		Debug.end("saving runes manager");
		objectMapper.writeValue(stream, memory.getData());
	}

	public static void onDisable() {
		save();
	}

	public static void save() {
//		Debug.start("save");
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			writeDataToBuffer(buffer);
			FileOutputStream fos = new FileOutputStream(dataFile);
			//write data.json
			fos.write(buffer.toByteArray(), 0, buffer.size());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Debug.end("save");
	}
}