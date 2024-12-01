package me.newyith.fortress.disruption;

import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class DisruptionManager {
	private static DisruptionManager instance = null;
	public static DisruptionManager getInstance() {
		if (instance == null) {
			instance = new DisruptionManager();
		}
		return instance;
	}
	public static void setInstance(DisruptionManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, DisruptionManagerForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, DisruptionManagerForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public DisruptionManagerForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new DisruptionManagerForWorld(world));
				} else {
					Debug.warn("DisruptionManager::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public DisruptionManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public DisruptionManager() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static DisruptionManagerForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}
}
