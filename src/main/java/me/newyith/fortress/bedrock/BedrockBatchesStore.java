package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class BedrockBatchesStore {
	private static BedrockBatchesStore instance = null;
	public static BedrockBatchesStore getInstance() {
		if (instance == null) {
			instance = new BedrockBatchesStore();
		}
		return instance;
	}
	public static void setInstance(BedrockBatchesStore newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, BedrockBatchesStoreForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, BedrockBatchesStoreForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public BedrockBatchesStoreForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new BedrockBatchesStoreForWorld(world));
				} else {
					Debug.warn("BedrockBatchesStore::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockBatchesStore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockBatchesStore() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static BedrockBatchesStoreForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}
}
