package me.newyith.fortress.main;

import me.newyith.fortress.command.StuckPlayer;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;

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

	public static boolean onNetherPortalTeleport(Player player, World fromWorld, Point fromPoint, World toWorld, Point toPoint) {
		boolean cancel = false;

		FortressesManagerForWorld fromWorldManager = forWorld(fromWorld);
		FortressesManagerForWorld toWorldManager = forWorld(toWorld);
		boolean canUseFromPortal = player.isSneaking() || fromWorldManager.playerCanUseNetherPortal(player, fromPoint);
		boolean canUseToPortal = toWorldManager.playerCanUseNetherPortal(player, toPoint);

		Debug.msg("canUseToPortal: " + canUseToPortal); //TODO:: delete this line

		if (!canUseFromPortal || !canUseToPortal) {
			cancel = true;

			if (player.isSneaking()) {
				boolean teleported = StuckPlayer.teleport(player, toWorld, toPoint);
				player.sendMessage(ChatColor.AQUA + "You were not whitelisted on destination portal. Forcing teleport to nearby.");
				if (!teleported) {
					player.sendMessage(ChatColor.AQUA + "Force teleport failed because no suitable destination was found.");
				}
			} else {
				String msgLine1 = "To enter/exit nether portal inside fortress, you must place whitelist sign(s) on the portal frame inside fortress.";
				String msgLine2 = "To "+ChatColor.DARK_AQUA+"force teleport"+ChatColor.AQUA+" to a location nearby destination portal, sneak (hold shift) while entering portal.";
				player.sendMessage(ChatColor.AQUA + msgLine1);
				player.sendMessage(ChatColor.AQUA + msgLine2);
			}
		}

		return cancel;
	}
}
