package me.newyith.fortressOrig.main;

import me.newyith.fortressOrig.rune.generator.GeneratorRune;
import org.bukkit.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class FortressesManager {
	private static FortressesManager instance = null;
	public static FortressesManager getInstance() {
		if (instance == null) {
			instance = new FortressesManager();
		}
		return instance;
	}
	public static void setInstance(FortressesManager newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private Map<String, FortressesManagerForWorld> managerByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("managerByWorld") Map<String, FortressesManagerForWorld> managerByWorld) {
			this.managerByWorld = managerByWorld;

			//rebuild transient fields
		}

		public FortressesManagerForWorld getManagerByWorldName(String worldName) {
			if (!managerByWorld.containsKey(worldName)) {
				managerByWorld.put(worldName, new FortressesManagerForWorld(Bukkit.getWorld(worldName)));
			}
			return managerByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public FortressesManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public FortressesManager() {
		model = new Model(new HashMap<>());
	}

	public static void secondStageLoad() {
		instance.model.managerByWorld.forEach((worldName, manager) -> {
			manager.secondStageLoad();
		});
	}

	//-----------------------------------------------------------------------

	public static FortressesManagerForWorld forWorld(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}

	// - Getters / Setters -

	public static Set<GeneratorRune> getRunesInAllWorlds() { //TODO: consider refactoring BedrockSafety so it iterators over worlds instead of doing here
		Set<GeneratorRune> runes = new HashSet<>();

		Set<String> worldNames = instance.model.managerByWorld.keySet();
		for (String worldName : worldNames) {
			FortressesManagerForWorld manager = instance.model.getManagerByWorldName(worldName);
			runes.addAll(manager.getRunes());
		}

		return runes;
	}

	public static int getRuneCountForAllWorlds() {
		int runeCount = 0;

		Set<String> worldNames = instance.model.managerByWorld.keySet();
		for (String worldName : worldNames) {
			FortressesManagerForWorld manager = instance.model.getManagerByWorldName(worldName);
			runeCount += manager.getRuneCount();
		}

		return runeCount;
	}

	// - Events -

	public static void onTick() {
		instance.model.managerByWorld.forEach((worldName, manager) -> {
			manager.onTick();
		});
	}
}
