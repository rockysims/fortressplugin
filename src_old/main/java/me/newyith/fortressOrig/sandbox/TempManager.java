package me.newyith.fortressOrig.sandbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortressOrig.bedrock.BedrockAuthToken;
import me.newyith.fortressOrig.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class TempManager {
	private static TempManager instance = null;
	public static TempManager getInstance() {
		if (instance == null) {
			instance = new TempManager();
		}
		return instance;
	}
	public static void setInstance(TempManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, TempManagerForWorld> managerByWorld = null;
		private String phString = "myPhString";

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, TempManagerForWorld> managerByWorld,
					 @JsonProperty("phString") String phString) {
			this.managerByWorld = managerByWorld;
			this.phString = phString;

			//rebuild transient fields
		}

		public TempManagerForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new TempManagerForWorld(world));
				} else {
					Debug.warn("TempManager::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public TempManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TempManager() {
		model = new Model(new HashMap<>(), "phStringFromConstructor");
	}

	//-----------------------------------------------------------------------

	public static TempManagerForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}

	public void init(World world, BedrockAuthToken bedrockAuthToken) {
		forWorld(world).init(bedrockAuthToken);
	}
}
