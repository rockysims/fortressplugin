package me.newyith.fortress.bedrock;

import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class BedrockManagerNew {
	private static BedrockManagerNew instance = null;
	public static BedrockManagerNew getInstance() {
		if (instance == null) {
			instance = new BedrockManagerNew();
		}
		return instance;
	}
	public static void setInstance(BedrockManagerNew newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, BedrockManagerNewForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, BedrockManagerNewForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public BedrockManagerNewForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new BedrockManagerNewForWorld(world));
				} else {
					Debug.warn("BedrockManagerNew::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockManagerNew(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockManagerNew() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static BedrockManagerNewForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}

	// - Events -

	public static void onTick() {
		instance.model.managerByWorld.forEach((worldName, manager) -> {
			manager.onTick();
		});
	}
}
