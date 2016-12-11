package me.newyith.fortress.protection;

import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ProtectionManager {
	private static ProtectionManager instance = null;
	public static ProtectionManager getInstance() {
		if (instance == null) {
			instance = new ProtectionManager();
		}
		return instance;
	}
	public static void setInstance(ProtectionManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, ProtectionManagerForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, ProtectionManagerForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public ProtectionManagerForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					managerByWorld.put(worldName, new ProtectionManagerForWorld(world));
				} else {
					Debug.warn("ProtectionManager::getManagerByWorldName() failed to find world named: " + worldName);
				}
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public ProtectionManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public ProtectionManager() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static ProtectionManagerForWorld forWorld(World world) {
		if (instance == null) Debug.msg("null a");
		if (instance.model == null) Debug.msg("null b");



		return instance.model.getManagerByWorldName(world.getName());
	}
}
