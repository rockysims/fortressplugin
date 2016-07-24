package me.newyith.fortress.main;

import me.newyith.fortress.core.BaseCore;
import me.newyith.fortress.core.BedrockManager;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.rune.generator.GeneratorRunePattern;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Blocks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.*;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

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

	private static FortressesManagerForWorld getManager(World world) {
		return instance.model.getManagerByWorldName(world.getName());
	}

	// - Getters / Setters -

	public static GeneratorRune getRune(World w, Point p) {
		return getManager(w).getRune(p);
	}

	//this helps separate Rune and Core (kind of a hack to find core through rune. fix later)
	public static BaseCore getCore(World w, Point p) {
		return getManager(w).getCore(p);
	}

	public static Set<GeneratorRune> getRunesInAllWorlds() { //TODO: consider refactoring BedrockSafety so it iterators over worlds instead of doing here
		Set<GeneratorRune> runes = new HashSet<>();

		Set<String> worldNames = instance.model.managerByWorld.keySet();
		for (String worldName : worldNames) {
			FortressesManagerForWorld manager = instance.model.getManagerByWorldName(worldName);
			runes.addAll(manager.getRunes());
		}

		return runes;
	}

	//during /fort stuck, we need all generators player might be inside so we can search by fortress cuboids
	public static Set<GeneratorRune> getGeneratorRunesNear(World w, Point center) {
		return getManager(w).getGeneratorRunesNear(center);
	}

	//during generation, we need all potentially conflicting generators (not just known ones) so search by range
	public static Set<BaseCore> getOtherCoresInRange(World w, Point center, int range) {
		return getManager(w).getOtherCoresInRange(center, range);
	}

	public static void addProtectedPoint(World w, Point p, Point anchor) {
		getManager(w).addProtectedPoint(p, anchor);
	}

	public static void removeProtectedPoint(World w, Point p) {
		getManager(w).removeProtectedPoint(p);
	}

	public static void addAlteredPoint(World w, Point p) {
		getManager(w).addAlteredPoint(p);
	}

	public static void removeAlteredPoint(World w, Point p) {
		getManager(w).removeAlteredPoint(p);
	}

	public static boolean isGenerated(World w, Point p) {
		return getManager(w).isGenerated(p);
	}

	public static boolean isAltered(World w, Point p) {
		return getManager(w).isAltered(p);
	}

	public static boolean isClaimed(World w, Point p) {
		return getManager(w).isClaimed(p);
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

	public static boolean onSignChange(Player player, Block signBlock) {
		World w = signBlock.getWorld();
		return getManager(w).onSignChange(player, signBlock);
	}

	public static void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		World w = event.getBlock().getWorld();
		getManager(w).onBlockRedstoneEvent(event);
	}

	public static boolean onExplode(List<Block> explodeBlocks, Location loc, float yield) {
		World w = loc.getWorld();
		return getManager(w).onExplode(explodeBlocks, loc, yield);
	}

	public static void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		World w = event.getPlayer().getWorld();
		getManager(w).onPlayerOpenCloseDoor(event);
	}

	public static void onBlockBreakEvent(BlockBreakEvent event) {
		World w = event.getBlock().getWorld();
		getManager(w).onBlockBreakEvent(event);
	}
	public static void onEnvironmentBreaksRedstoneWireEvent(Block brokenBlock) {
		World w = brokenBlock.getWorld();
		getManager(w).onEnvironmentBreaksRedstoneWireEvent(brokenBlock);
	}
	public static boolean onBlockPlaceEvent(Player player, Block placedBlock, Material replacedMaterial) {
		World w = placedBlock.getWorld();
		return getManager(w).onBlockPlaceEvent(player, placedBlock, replacedMaterial);
	}

	public static boolean onPistonEvent(boolean isSticky, World world, Point piston, Point target, Set<Block> movedBlocks) {
		return getManager(world).onPistonEvent(isSticky, world, piston, target, movedBlocks);
	}

	public static void breakRune(GeneratorRune rune) {
		World w = rune.getPattern().getWorld();
		getManager(w).breakRune(rune);
	}
}
