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

//fully written again
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
		private Set<GeneratorRune> generatorRunes = null;
		private transient Map<String, Map<Point, GeneratorRune>> generatorRuneByPatternPointByWorld = null;
		private transient Map<String, Map<Point, GeneratorRune>> generatorRuneByProtectedPointByWorld = null;
		private transient Map<String, Set<Point>> protectedPointsByWorld = null;
		private transient Map<String, Set<Point>> alteredPointsByWorld = null;

		@JsonCreator
		public Model(@JsonProperty("generatorRunes") Set<GeneratorRune> generatorRunes) {
			this.generatorRunes = generatorRunes;

			//rebuild transient fields
			generatorRuneByPatternPointByWorld = new HashMap<>();
			generatorRuneByProtectedPointByWorld = new HashMap<>();
			protectedPointsByWorld = new HashMap<>();
			alteredPointsByWorld = new HashMap<>();
			for (GeneratorRune rune : generatorRunes) {
				World world = rune.getPattern().getWorld();

				//rebuild runeByPoint map
				for (Point p : rune.getPattern().getPoints()) {
					getGeneratorRuneByPatternPointMap(world).put(p, rune);
				}

				//rebuild alteredPoints
				Set<Point> altereds = rune.getGeneratorCore().getAlteredPoints();
				getAlteredPoints(world).addAll(altereds);

				//rebuild protectedPoints
				Set<Point> protecteds = rune.getGeneratorCore().getProtectedPoints();
				getProtectedPoints(world).addAll(protecteds);

				//rebuild runeByProtectedPoint
				for (Point p : protecteds) {
					getGeneratorRuneByProtectedPointMap(world).put(p, rune);
				}
			}
		}

		public Map<Point, GeneratorRune> getGeneratorRuneByPatternPointMap(World w) {
			String worldName = w.getName();
			if (!generatorRuneByPatternPointByWorld.containsKey(worldName)) {
				generatorRuneByPatternPointByWorld.put(worldName, new HashMap<>());
			}
			return generatorRuneByPatternPointByWorld.get(worldName);
		}

		public Map<Point, GeneratorRune> getGeneratorRuneByProtectedPointMap(World w) {
			String worldName = w.getName();
			if (!generatorRuneByProtectedPointByWorld.containsKey(worldName)) {
				generatorRuneByProtectedPointByWorld.put(worldName, new HashMap<>());
			}
			return generatorRuneByProtectedPointByWorld.get(worldName);
		}

		public Set<Point> getProtectedPoints(World w) {
			String worldName = w.getName();
			if (!protectedPointsByWorld.containsKey(worldName)) {
				protectedPointsByWorld.put(worldName, new HashSet<>());
			}
			return protectedPointsByWorld.get(worldName);
		}

		public Set<Point> getAlteredPoints(World w) {
			String worldName = w.getName();
			if (!alteredPointsByWorld.containsKey(worldName)) {
				alteredPointsByWorld.put(worldName, new HashSet<>());
			}
			return alteredPointsByWorld.get(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public FortressesManager(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public FortressesManager() {
		model = new Model(new HashSet<>());
	}

	public static void secondStageLoad() {
		//pass along secondStageLoad event to runes
		for (GeneratorRune rune : instance.model.generatorRunes) {
			rune.secondStageLoad();
		}

		//break any invalid runes
		Set<GeneratorRune> generatorRunesCopy = new HashSet<>(instance.model.generatorRunes);
		for (GeneratorRune rune : generatorRunesCopy) {
			if (!rune.getPattern().isValid()) {
				breakRune(rune);
			}
		}
	}

	//-----------------------------------------------------------------------

	// - Getters / Setters -

	//TODO: fix PROBLEM: we need to know the world too and there can be multiple runes per point
	//	maybe not a big deal since that just means you can't create a rune at a point if point is already taken in another world
	//	should still fix it (maybe something like runeByPoint[world].get(p) instead of runeByPoint.get(p)?)
	public static GeneratorRune getRune(World w, Point p) {
		return instance.model.getGeneratorRuneByPatternPointMap(w).get(p);
	}

	//this helps separate Rune and Core (kind of a hack to find core through rune. fix later)
	public static BaseCore getCore(World w, Point p) {
		BaseCore core = null;

		GeneratorRune rune = getRune(w, p);
		if (rune != null) {
			core = rune.getGeneratorCore();
		}

		return core;
	}

	public static Set<GeneratorRune> getRunes() {
		return instance.model.generatorRunes;
	}

	//during /fort stuck, we need all generators player might be inside so we can search by fortress cuboids
	public static Set<GeneratorRune> getGeneratorRunesNear(Point center) {
		Set<GeneratorRune> overlapRunes = new HashSet<>();

		//overlapRunes = runes where fortress cuboid contains 'center' point
		for (GeneratorRune rune : instance.model.generatorRunes) {
			if (rune.getFortressCuboid().contains(center)) {
				overlapRunes.add(rune);
			}
		}

		return overlapRunes;
	}

	//during generation, we need all potentially conflicting generators (not just known ones) so search by range
	public static Set<BaseCore> getOtherCoresInRange(World world, Point center, int range) {
		Set<BaseCore> coresInRange = new HashSet<>();

		//fill runesInRange
		for (GeneratorRune rune : instance.model.generatorRunes) {
			//set inRange
			boolean inRange = true;
			World w = rune.getPattern().getWorld();
			Point p = rune.getPattern().getAnchorPoint();
			inRange = inRange && w.getName() == world.getName();
			inRange = inRange && Math.abs(p.xInt() - center.xInt()) <= range;
			inRange = inRange && Math.abs(p.yInt() - center.yInt()) <= range;
			inRange = inRange && Math.abs(p.zInt() - center.zInt()) <= range;

			if (inRange) {
				coresInRange.add(rune.getGeneratorCore());
			}
		}
		coresInRange.remove(getRune(world, center).getGeneratorCore());
		return coresInRange;
	}

	public static void addProtectedPoint(World w, Point p, Point anchor) {
		instance.model.getProtectedPoints(w).add(p);
		instance.model.getGeneratorRuneByProtectedPointMap(w).put(p, getRune(w, anchor));
	}

	public static void removeProtectedPoint(World w, Point p) {
		instance.model.getProtectedPoints(w).remove(p);
		instance.model.getGeneratorRuneByProtectedPointMap(w).remove(p);
	}

	public static void addAlteredPoint(World w, Point p) {
		instance.model.getAlteredPoints(w).add(p);
	}

	public static void removeAlteredPoint(World w, Point p) {
		instance.model.getAlteredPoints(w).remove(p);
	}

	public static boolean isGenerated(World w, Point p) {
		return instance.model.getProtectedPoints(w).contains(p) || instance.model.getAlteredPoints(w).contains(p);
	}

	public static boolean isAltered(World w, Point p) {
		return instance.model.getAlteredPoints(w).contains(p);
	}

	public static boolean isClaimed(Point p) {
		boolean claimed = false;

		Iterator<GeneratorRune> it = instance.model.generatorRunes.iterator();
		while (it.hasNext()) {
			GeneratorRune rune = it.next();
			claimed = rune.getGeneratorCore().getClaimedPoints().contains(p);
			if (claimed) {
				break;
			}
		}

		return claimed;
	}

	public static int getRuneCount() {
		return instance.model.generatorRunes.size();
	}

	// - Events -

	public static void onTick() {
		instance.model.generatorRunes.forEach(GeneratorRune::onTick);
	}

	public static boolean onSignChange(Player player, Block signBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryReadyPattern(signBlock);
		if (runePattern != null) {
			World world = runePattern.getWorld();
			boolean runeAlreadyCreated = instance.model.getGeneratorRuneByPatternPointMap(world).containsKey(new Point(signBlock));
			if (!runeAlreadyCreated) {
				GeneratorRune rune = new GeneratorRune(runePattern);
				instance.model.generatorRunes.add(rune);

				//add new rune to generatorRuneByPoint map
				for (Point p : runePattern.getPoints()) {
					instance.model.getGeneratorRuneByPatternPointMap(world).put(p, rune);
				}

				rune.onCreated(player);
				cancel = true; //otherwise initial text on sign is replaced by what user wrote
			} else {
				//TODO: consider coloring this message or better yet abstracting sendMsg among all classes
				player.sendMessage("Failed to create rune because rune already created here.");
			}
		}

		return cancel;
	}

	public static void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		int signal = event.getNewCurrent();
		Block block = event.getBlock();
		World world = block.getWorld();
		Point p = new Point(block);

		//if the redstone that changed is part of the rune, update rune state
		if (instance.model.getGeneratorRuneByPatternPointMap(world).containsKey(p)) {
			GeneratorRune rune = instance.model.getGeneratorRuneByPatternPointMap(world).get(p);
			rune.setPowered(signal > 0);
		}

		//if door is protected, ignore redstone event
		if (Blocks.isDoor(block.getType()) && instance.model.getProtectedPoints(world).contains(p)) {
			Openable openableDoor = (Openable)block.getState().getData();
			if (openableDoor.isOpen()) {
				event.setNewCurrent(1);
			} else {
				event.setNewCurrent(0);
			}
		}
	}

	public static boolean onExplode(List<Block> explodeBlocks, Location loc, float yield) {
		boolean cancel = false;
		World world = loc.getWorld();

		Set<Point> pointsToShield = new HashSet<>();
		Iterator<Block> it = explodeBlocks.iterator();
		while (it.hasNext()) {
			Point p = new Point(it.next());
			if (isGenerated(world, p) && !p.is(Material.BEDROCK, loc.getWorld())) {
				pointsToShield.add(p);
			}
		}

		if (!pointsToShield.isEmpty()) {
			//pointsToShield excludes bedrock so we know points are not already converted
			for (Point p : pointsToShield) {
				BedrockManager.convert(world, p);
			}

			loc.getWorld().createExplosion(loc, yield);

			/*
			Random random = new Random();
			for (Point p : pointsToShield) {
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
				BedrockManager.revert(world, p);
			}, 25 + random.nextInt(12)); //20 ticks per second
			}
			/*/
			for (Point p : pointsToShield) {
				BedrockManager.revert(world, p);
			}
			//*/

			cancel = true;
		}

//		Debug.msg("onExplode() returning " + String.valueOf(cancel));
		return cancel;
	}

	public static void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Block doorBlock = event.getClickedBlock();
		Point doorPoint = new Point(doorBlock.getLocation());
		World world = doorBlock.getWorld();

		GeneratorRune rune = instance.model.getGeneratorRuneByProtectedPointMap(world).get(doorPoint);
		if (rune != null) {
			Player player = event.getPlayer();
			Point aboveDoorPoint = new Point(doorPoint).add(0, 1, 0);
			switch (aboveDoorPoint.getBlock(world).getType()) {
				case IRON_DOOR_BLOCK:
				case WOODEN_DOOR:
				case ACACIA_DOOR:
				case BIRCH_DOOR:
				case DARK_OAK_DOOR:
				case JUNGLE_DOOR:
				case SPRUCE_DOOR:
					//ignore trap doors since they're only 1 high
					doorPoint = aboveDoorPoint;
			}
			boolean canOpen = rune.getGeneratorCore().playerCanOpenDoor(player, doorPoint);
			if (!canOpen) {
				event.setCancelled(true);
			} else {
				//if iron door, open for player
				Material doorType = doorPoint.getBlock(world).getType();
				boolean isIronDoor = doorType == Material.IRON_DOOR_BLOCK;
				boolean isIronTrap = doorType == Material.IRON_TRAPDOOR;
				if (isIronDoor || isIronTrap) {
					BlockState state = doorBlock.getState();
					boolean nowOpen;
					if (isIronDoor) {
						Door door = (Door) state.getData();
						if (door.isTopHalf()) {
							Block bottomDoorBlock = (new Point(doorPoint).add(0, -1, 0)).getBlock(world);
							state = bottomDoorBlock.getState();
							door = (Door) state.getData();
						}
						door.setOpen(!door.isOpen());
						state.update();
						nowOpen = door.isOpen();
					} else {
						TrapDoor door = (TrapDoor) state.getData();
						door.setOpen(!door.isOpen());
						state.update();
						nowOpen = door.isOpen();
					}
					if (nowOpen) {
						player.playSound(doorPoint.toLocation(world), Sound.DOOR_OPEN, 1.0F, 1.0F);
					} else {
						player.playSound(doorPoint.toLocation(world), Sound.DOOR_CLOSE, 1.0F, 1.0F);
					}
				}
			}
		}
	}

	public static void onBlockBreakEvent(BlockBreakEvent event) {
		Block brokenBlock = event.getBlock();
		Point brokenPoint = new Point(brokenBlock);
		World world = brokenBlock.getWorld();

		boolean isProtected = instance.model.getProtectedPoints(world).contains(brokenPoint);
		boolean inCreative = event.getPlayer().getGameMode() == GameMode.CREATIVE;
		boolean cancel = false;
		if (isProtected && !inCreative) {
			cancel = true;
		} else {
			if (brokenPoint.is(Material.PISTON_EXTENSION, world) || brokenPoint.is(Material.PISTON_MOVING_PIECE, world)) {
				MaterialData matData = brokenBlock.getState().getData();
				if (matData instanceof PistonExtensionMaterial) {
					PistonExtensionMaterial pem = (PistonExtensionMaterial) matData;
					BlockFace face = pem.getFacing().getOppositeFace();
					Point pistonPoint = new Point(brokenBlock.getRelative(face, 1));
					if (instance.model.getProtectedPoints(world).contains(pistonPoint)) {
						cancel = true;
					}
				} else {
					Debug.error("matData not instanceof PistonExtensionMaterial");
				}
			}
		}

		if (cancel) {
			event.setCancelled(true);
		} else {
			onRuneMightHaveBeenBrokenBy(brokenBlock);
		}
	}
	public static void onEnvironmentBreaksRedstoneWireEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}
	public static boolean onBlockPlaceEvent(Player player, Block placedBlock, Material replacedMaterial) {
		boolean cancel = false;

		World world = placedBlock.getWorld();
		switch (replacedMaterial) {
			case STATIONARY_WATER:
			case STATIONARY_LAVA:
				Point placedPoint = new Point(placedBlock);
				boolean isProtected = instance.model.getProtectedPoints(world).contains(placedPoint);
				boolean inCreative = player.getGameMode() == GameMode.CREATIVE;
				if (isProtected && !inCreative) {
					cancel = true;
				}
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(placedBlock);
		}

		return cancel;
	}
	private static void onRuneMightHaveBeenBrokenBy(Block block) {
		World world = block.getWorld();
		Point p = new Point(block);
		if (instance.model.getGeneratorRuneByPatternPointMap(world).containsKey(p)) {
			GeneratorRune rune = instance.model.getGeneratorRuneByPatternPointMap(world).get(p);

			if (rune.getPattern().contains(p)) {
				breakRune(rune);
			}
		}
	}

	public static boolean onPistonEvent(boolean isSticky, World world, Point piston, Point target, Set<Block> movedBlocks) {
		boolean cancel = false;

		if (movedBlocks != null) {
			for (Block movedBlock : movedBlocks) {
				Point movedPoint = new Point(movedBlock);
				if (instance.model.getProtectedPoints(world).contains(movedPoint)) {
					cancel = true;
				}
			}
		}

		if (!cancel) {
			//build pointsAffected
			HashSet<Point> pointsAffected = new HashSet<>();
			pointsAffected.add(piston);
			if (target != null) {
				pointsAffected.add(target);
			}
			if (movedBlocks != null) {
				for (Block b : movedBlocks) {
					pointsAffected.add(new Point(b));
				}
			}

			//build runesAffected
			HashSet<GeneratorRune> runesAffected = new HashSet<>();
			for (Point p : pointsAffected) {
				GeneratorRune rune = getRune(world, p);
				if (rune != null) {
					runesAffected.add(rune);
				}
			}

			//break affected runes
			for (GeneratorRune rune : runesAffected) {
				breakRune(rune);
			}

			//Debug.msg("piston: " + piston);
			//Debug.msg("target: " + target);
			//Debug.msg("target type: " + target.getBlock().getType());
			Debug.msg("movedBlocks.size(): " + movedBlocks.size());
		}

		return cancel;
	}

	public static void breakRune(GeneratorRune rune) {
		World world = rune.getPattern().getWorld();
		Set<Point> patternPoints = rune.getPattern().getPoints();

		rune.onBroken();
		//breaking should naturally update: alteredPoints, protectedPoints, and generatorRuneByProtectedPoint

		for (Point p : patternPoints) {
			instance.model.getGeneratorRuneByPatternPointMap(world).remove(p);
		}

		instance.model.generatorRunes.remove(rune);
	}
}
