package me.newyith.fortress.bedrock;

import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class BedrockManager {
	private static BedrockManager instance = null;
	public static BedrockManager getInstance() {
		if (instance == null) {
			instance = new BedrockManager();
		}
		return instance;
	}
	public static void setInstance(BedrockManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, BedrockManagerForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, BedrockManagerForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public BedrockManagerForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new BedrockManagerForWorld(world));
				} else {
					Debug.warn("BedrockManager::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManager() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static BedrockManagerForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}

	// - Events -

	public static void onTick() {
		instance.model.managerByWorld.forEach((worldName, manager) -> {
			manager.onTick();
		});
	}
}
