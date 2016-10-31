package me.newyith.fortress.bedrock.timed;

import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class TimedBedrockManagerNew {
	private static TimedBedrockManagerNew instance = null;
	public static TimedBedrockManagerNew getInstance() {
		if (instance == null) {
			instance = new TimedBedrockManagerNew();
		}
		return instance;
	}
	public static void setInstance(TimedBedrockManagerNew newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, TimedBedrockManagerNewForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, TimedBedrockManagerNewForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public TimedBedrockManagerNewForWorld getManagerByWorld(World world) {
			String worldName = world.getName();
			if (!managerByWorld.containsKey(worldName)) {
				managerByWorld.put(worldName, new TimedBedrockManagerNewForWorld(world));
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockManagerNew(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TimedBedrockManagerNew() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static TimedBedrockManagerNewForWorld forWorld(World world) {
		return instance.model.getManagerByWorld(world);
	}
}
