package me.newyith.fortressOrig.bedrock.timed;

import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class TimedBedrockManager {
	private static TimedBedrockManager instance = null;
	public static TimedBedrockManager getInstance() {
		if (instance == null) {
			instance = new TimedBedrockManager();
		}
		return instance;
	}
	public static void setInstance(TimedBedrockManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, TimedBedrockManagerForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, TimedBedrockManagerForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public TimedBedrockManagerForWorld getManagerByWorld(World world) {
			String worldName = world.getName();
			TimedBedrockManagerForWorld manager = managerByWorld.get(worldName);
			if (manager == null) {
				manager = new TimedBedrockManagerForWorld(world);
				managerByWorld.put(worldName, manager);
			}
			return manager;
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockManager() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static TimedBedrockManagerForWorld forWorld(World world) {
		return instance.model.getManagerByWorld(world);
	}

	public static void onTick() {
		instance.model.managerByWorld.entrySet().stream().forEach((entry) -> {
			TimedBedrockManagerForWorld manager = entry.getValue();
			manager.onTick();
		});
	}
}
