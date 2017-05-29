package me.newyith.fortressOrig.main;

import me.newyith.fortressOrig.command.StuckPlayer;
import me.newyith.fortressOrig.core.BaseCore;
import me.newyith.fortressOrig.protection.ProtectionManager;
import me.newyith.fortressOrig.rune.generator.GeneratorRune;
import me.newyith.fortressOrig.rune.generator.GeneratorRunePattern;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Debug;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.*;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class FortressesManagerForWorld {
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
		private final String worldName;
		private final transient World world;
		private transient Map<Point, GeneratorRune> generatorRuneByPatternPoint = null;
		private transient Map<Point, GeneratorRune> generatorRuneByClaimedWallPoint = null;

		@JsonCreator
		public Model(@JsonProperty("generatorRunes") Set<GeneratorRune> generatorRunes,
					 @JsonProperty("worldName") String worldName) {
			this.generatorRunes = generatorRunes;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			generatorRuneByPatternPoint = new HashMap<>();
			generatorRuneByClaimedWallPoint = new HashMap<>();
			for (GeneratorRune rune : generatorRunes) {
				//rebuild runeByPoint map
				for (Point p : rune.getPattern().getPoints()) {
					generatorRuneByPatternPoint.put(p, rune);
				}

				//rebuild generatorRuneByClaimedWallPoint map
				for (Point p : rune.getGeneratorCore().getClaimedWallPoints()) {
					generatorRuneByClaimedWallPoint.put(p, rune);
				}
			}
		}
	}
	private Model model = null;

	@JsonCreator
	public FortressesManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public FortressesManagerForWorld(World world) {
		model = new Model(new HashSet<>(), world.getName());
	}

	public void secondStageLoad() {
		//pass along secondStageLoad event to runes
		for (GeneratorRune rune : model.generatorRunes) {
			rune.secondStageLoad();
		}

		//break any invalid runes
		Set<GeneratorRune> generatorRunesCopy = new HashSet<>(model.generatorRunes);
		for (GeneratorRune rune : generatorRunesCopy) {
			if (!rune.getPattern().isValid()) {
				breakRune(rune);
			}
		}
	}

	//-----------------------------------------------------------------------

	// - Getters / Setters -

	public GeneratorRune getRuneByPatternPoint(Point p) {
		return model.generatorRuneByPatternPoint.get(p);
	}

	//this helps separate Rune and Core (kind of a hack to find core through rune. fix later)
	public BaseCore getCore(Point p) {
		BaseCore core = null;

		GeneratorRune rune = getRuneByPatternPoint(p);
		if (rune != null) {
			core = rune.getGeneratorCore();
		}

		return core;
	}

	public Set<GeneratorRune> getRunes() {
		return model.generatorRunes;
	}

	//during /stuck, we need all generators player might be inside so we can search by fortress cuboids
	public Set<GeneratorRune> getGeneratorRunesNear(Point center) {
		Set<GeneratorRune> overlapRunes = new HashSet<>();

		//overlapRunes = runes where fortress cuboid contains 'center' point
		for (GeneratorRune rune : model.generatorRunes) {
			if (rune.getFortressCuboid().contains(center)) {
				overlapRunes.add(rune);
			}
		}

		return overlapRunes;
	}

	//during generation, we need all potentially conflicting generators (not just known ones) so search by radius
	public Set<BaseCore> getOtherCoresInRadius(Point center, int radius) {
		Set<BaseCore> coresInRange = new HashSet<>();

		//fill runesInRange
		for (GeneratorRune rune : model.generatorRunes) {
			//set inRange
			boolean inRange = true;
			Point p = rune.getPattern().getAnchorPoint();
			inRange = inRange && Math.abs(p.xInt() - center.xInt()) <= radius;
			inRange = inRange && Math.abs(p.yInt() - center.yInt()) <= radius;
			inRange = inRange && Math.abs(p.zInt() - center.zInt()) <= radius;

			if (inRange) {
				coresInRange.add(rune.getGeneratorCore());
			}
		}
		coresInRange.remove(getRuneByPatternPoint(center).getGeneratorCore());
		return coresInRange;
	}

	public void addClaimedWallPoints(Set<Point> claimedWallPoints, Point anchor) {
//		Debug.msg("addClaimedWallPoints() claimedWallPoints.size(): " + claimedWallPoints.size());
		GeneratorRune rune = getRuneByPatternPoint(anchor);
		if (rune != null) {
			for (Point p : claimedWallPoints) {
				model.generatorRuneByClaimedWallPoint.put(p, rune);
			}
		} else {
			Debug.error("FortressesManagerForWorld::addClaimedWallPoints() failed to find rune associated with anchor: " + anchor);
		}
	}

	public void removeClaimedWallPoints(Set<Point> claimedWallPoints) {
//		Debug.msg("removeClaimedWallPoints() claimedWallPoints.size(): " + claimedWallPoints.size());
		claimedWallPoints.forEach(model.generatorRuneByClaimedWallPoint::remove);
	}

	//TODO: remove this method and call isProtected() directly
	public boolean isGenerated(Point p) {
		return ProtectionManager.forWorld(model.world).isProtected(p);
	}

	public boolean isClaimed(Point p) {
		boolean claimed = false;

		Iterator<GeneratorRune> it = model.generatorRunes.iterator();
		while (it.hasNext()) {
			GeneratorRune rune = it.next();
			claimed = rune.getGeneratorCore().getClaimedPoints().contains(p);
			if (claimed) {
				break;
			}
		}

		return claimed;
	}

	public int getRuneCount() {
		return model.generatorRunes.size();
	}

	// - Events -

	public void onTick() {
		model.generatorRunes.forEach(GeneratorRune::onTick);
	}

	public boolean onSignChange(Player player, Block signBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryReadyPattern(signBlock);
		if (runePattern != null) {
			boolean runeAlreadyCreated = model.generatorRuneByPatternPoint.containsKey(new Point(signBlock));
			if (!runeAlreadyCreated) {
				GeneratorRune rune = new GeneratorRune(runePattern);
				model.generatorRunes.add(rune);

				//add new rune to generatorRuneByPoint map
				for (Point p : runePattern.getPoints()) {
					model.generatorRuneByPatternPoint.put(p, rune);
				}

				rune.onCreated(player);
				cancel = true; //otherwise initial text on sign is replaced by what user wrote
			} else {
				player.sendMessage(ChatColor.AQUA + "Failed to create rune because rune already created here.");
			}
		}

		return cancel;
	}

	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		int signal = event.getNewCurrent();
		Block block = event.getBlock();
		Point p = new Point(block);

		//if the redstone that changed is part of the rune, update rune state
		if (model.generatorRuneByPatternPoint.containsKey(p)) {
			GeneratorRune rune = model.generatorRuneByPatternPoint.get(p);
			rune.setPowered(signal > 0);
		}

		//if door is generated, ignore redstone event
		if (Blocks.isDoor(block.getType()) && isGenerated(p)) {
			Openable openableDoor = (Openable)block.getState().getData();
			if (openableDoor.isOpen()) {
				event.setNewCurrent(1);
			} else {
				event.setNewCurrent(0);
			}
		}
	}

	public boolean onExplode(List<Block> explodeBlocks, Location loc) {
		boolean cancel = false;
		World world = loc.getWorld();

		boolean generatedBlockExploded = false;
		Iterator<Block> it = explodeBlocks.iterator();
		while (it.hasNext()) {
			Point p = new Point(it.next());
			if (isGenerated(p) && !p.is(Material.BEDROCK, world)) {
				generatedBlockExploded = true;
				break;
			}
		}

		if (generatedBlockExploded) {
			Set<Point> allPointsToShield = new HashSet<>();
			Vector explosionOrigin = loc.toVector();
			it = explodeBlocks.iterator();
			while (it.hasNext()) {
				Point explodePoint = new Point(it.next());

				//if (generated block between explosion and explodePoint)
				//	add generated block to allPointsToShield
				//	remove explodePoint from explodeBlocks
				Vector direction = explodePoint.add(0.5, 0.5, 0.5).toVector().subtract(explosionOrigin);
				int distance = Math.max(1, (int) explosionOrigin.distance(explodePoint.toVector()));
				BlockIterator rayBlocks = new BlockIterator(world, explosionOrigin, direction, 0, distance);
				while (rayBlocks.hasNext()) {
					Block rayBlock = rayBlocks.next();
					Point rayPoint = new Point(rayBlock);
					boolean rayBlockIsShield = isGenerated(rayPoint) && !rayPoint.is(Material.BEDROCK, world);
					if (rayBlockIsShield) {
						allPointsToShield.add(rayPoint);
						it.remove(); //remove explodePoint from explodeBlocks since rayBlockIsShield
						break;
					}
				}
			}

			//remove generated blocks from explodeBlocks
			it = explodeBlocks.iterator();
			while (it.hasNext()) {
				Point p = new Point(it.next());
				if (isGenerated(p) && !p.is(Material.BEDROCK, world)) {
					it.remove(); //remove generated block from explodeBlocks
				}
			}

			//show shield bedrock
			if (!allPointsToShield.isEmpty()) {
				//fill pointsToShieldByRune from allPointsToShield
				Map<GeneratorRune, Set<Point>> pointsToShieldByRune = new HashMap<>();
				for (Point p : allPointsToShield) {
					GeneratorRune rune = getRuneByClaimedWallPoint(p);
					if (rune != null) { //should always be true in theory
						Set<Point> runePointsToShield = pointsToShieldByRune.get(rune);
						if (runePointsToShield == null) {
							runePointsToShield = new HashSet<>();
							pointsToShieldByRune.put(rune, runePointsToShield);
						}
						runePointsToShield.add(p);
					} else {
						Debug.warn("onExplode() failed to show shield because rune == null at " + p);
					}
				}

				for (Map.Entry<GeneratorRune, Set<Point>> entry : pointsToShieldByRune.entrySet()) {
					GeneratorRune rune = entry.getKey();
					Set<Point> runePointsToShield = entry.getValue();
					rune.getGeneratorCore().shield(runePointsToShield);
				}
			}
		}

		//break runes if needed
		explodeBlocks.forEach(this::onRuneMightHaveBeenBrokenBy);

		return cancel;
	}

	public boolean onIgnite(Block b) {
		boolean cancel = false;

		Point p = new Point(b);
		boolean igniteProof = isClaimed(p);
		if (igniteProof) {
			//TODO: uncomment out once issue where /reload causes delayed task to be forgotten is fixed
//			BedrockManager.convert(w, p);
//			Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
//				BedrockManager.revert(w, p);
//			}, 25 + 15);

//			Debug.msg("cancel ignite at " + p);
//			Bukkit.getOnlinePlayers().forEach(player -> {
//				player.sendMessage("cancel ignite at " + p);
//			});

			cancel = true;
		}

		return cancel;
	}

	public boolean onBurn(Block b) {
		boolean cancel = false;

		World w = b.getWorld();
		Point p = new Point(b);
		boolean burnProof = FortressesManager.forWorld(w).isGenerated(p);
		if (burnProof) {
			//TODO: uncomment out once issue where /reload causes delayed task to be forgotten is fixed
//			BedrockManager.convert(w, p);
//			Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
//				BedrockManager.revert(w, p);
//			}, 25 + 15);

//			Debug.msg("cancel burn at " + p);
//			Bukkit.getOnlinePlayers().forEach(player -> {
//				player.sendMessage("cancel burn at " + p);
//			});
			cancel = true;
		}

		return cancel;
	}

	public boolean onPlayerOpenCloseDoor(Player player, Block doorBlock) {
		boolean cancel = false;

		Point doorPoint = new Point(doorBlock.getLocation());
		World world = doorBlock.getWorld();

		if (isGenerated(doorPoint)) {
			GeneratorRune rune = getRuneByClaimedWallPoint(doorPoint);
			if (rune != null) {
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
					cancel = true;
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
			} else {
				Debug.error("FortressesManagerForWorld::onPlayerOpenCloseDoor() failed to find rune from doorPoint " + doorPoint);
			}
		}

		return cancel;
	}

	public void onPlayerRightClickBlock(Player player, Block block, BlockFace face) {
		Point p = new Point(block);
		GeneratorRune rune = getRuneByClaimedWallPoint(p);
		if (rune != null) {
			rune.onPlayerRightClickWall(player, block, face);
		}
	}

	private GeneratorRune getRuneByClaimedWallPoint(Point p) {
		return model.generatorRuneByClaimedWallPoint.get(p);
	}

	public void onBlockBreakEvent(BlockBreakEvent event) {
		Block brokenBlock = event.getBlock();
		Point brokenPoint = new Point(brokenBlock);

		boolean cancel = false;
		boolean inCreative = event.getPlayer().getGameMode() == GameMode.CREATIVE;
		if (!inCreative) {
			GeneratorRune rune = getRuneByClaimedWallPoint(brokenPoint);
			if (isGenerated(brokenPoint)) {
				cancel = true;
				if (rune != null) rune.getGeneratorCore().shield(brokenPoint);
			}

			//commented out because we're not gonna bother handling piston special case yet (handling it here is not elegant)
//			if (!isProtected) {
//				switch (brokenPoint.getType(world)) {
//					case PISTON_EXTENSION:
//					case PISTON_MOVING_PIECE:
//						MaterialData matData = brokenBlock.getState().getData();
//						if (matData instanceof PistonExtensionMaterial) {
//							PistonExtensionMaterial pem = (PistonExtensionMaterial) matData;
//							BlockFace face = pem.getFacing().getOppositeFace();
//							Point pistonBasePoint = new Point(brokenBlock.getRelative(face, 1));
//							if (model.protectedPoints.contains(pistonBasePoint)) {
//								cancel = true;
//								if (rune != null) rune.getGeneratorCore().shield(brokenPoint);
//							}
//						} else {
//							Debug.error("matData not instanceof PistonExtensionMaterial");
//						}
//				}
//			}
		}

		if (cancel) {
			event.setCancelled(true);
		} else {
			onRuneMightHaveBeenBrokenBy(brokenBlock);
		}
	}
	public void onEnvironmentBreaksRedstoneWireEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}
	public boolean onBlockPlaceEvent(Player player, Block placedBlock, Material replacedMaterial) {
		boolean cancel = false;

		switch (replacedMaterial) {
			case STATIONARY_WATER:
			case STATIONARY_LAVA:
				Point placedPoint = new Point(placedBlock);
				boolean isGenerated = isGenerated(placedPoint);
				boolean inCreative = player.getGameMode() == GameMode.CREATIVE;
				if (isGenerated && !inCreative) {
					cancel = true;
				}
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(placedBlock);
		}

		return cancel;
	}
	private void onRuneMightHaveBeenBrokenBy(Block block) {
		Point p = new Point(block);
		if (model.generatorRuneByPatternPoint.containsKey(p)) {
			GeneratorRune rune = model.generatorRuneByPatternPoint.get(p);

			if (rune.getPattern().contains(p)) {
				breakRune(rune);
			}
		}
	}

	public boolean onPistonEvent(boolean isSticky, Point piston, Point target, Set<Block> movedBlocks) {
		boolean cancel = false;

		if (movedBlocks != null) {
			for (Block movedBlock : movedBlocks) {
				Point movedPoint = new Point(movedBlock);
				if (isGenerated(movedPoint)) {
					cancel = true;
				}
			}
		}

		if (!cancel) {
			//scan pointsAffected
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

			//scan runesAffected
			HashSet<GeneratorRune> runesAffected = new HashSet<>();
			for (Point p : pointsAffected) {
				GeneratorRune rune = getRuneByPatternPoint(p);
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
//			Debug.msg("movedBlocks.size(): " + movedBlocks.size());
		}

		return cancel;
	}

	public boolean onEndermanPickupBlock(Block block) {
		boolean cancel = false;

		Point p = new Point(block);
		if (isGenerated(p)) {
			cancel = true;
		}

		return cancel;
	}

	public boolean onZombieBreakBlock(Block block) {
		boolean cancel = false;

		Point p = new Point(block);
		if (isGenerated(p)) {
			cancel = true;
		}

		return cancel;
	}

	public void onPlayerCloseChest(Player player, Block block) {
		Point p = new Point(block);
		GeneratorRune rune = getRuneByPatternPoint(p);
		if (rune != null) {
			rune.onPlayerCloseChest(player, p);
		}
	}

	public boolean onPlayerExitVehicle(Player player) {
		boolean cancel = false;

		//if (player in generated point) stuck teleport away immediately with message
		World w = player.getWorld();
		Point eyesPoint = new Point(player.getEyeLocation());
		Point feetPoint = eyesPoint.add(0, -1, 0);
		boolean eyesInGenerated = FortressesManager.forWorld(w).isGenerated(eyesPoint);
		boolean feetInGenerated = FortressesManager.forWorld(w).isGenerated(feetPoint);
		if (eyesInGenerated || feetInGenerated) {
			boolean teleported = StuckPlayer.teleport(player);
			player.sendMessage(ChatColor.AQUA + "You got stuck in fortress wall.");
			if (!teleported) {
				player.sendMessage(ChatColor.AQUA + "Stuck teleport failed because no suitable destination was found.");
				cancel = true; //canceling would allow trap minecarts (except /spawn should get you out, right?)
//									not canceling would allow forced fortress entry (if enemy can scan freely above and below fortress)
			}
		}

		return cancel;
	}

	public boolean onEntityDamageFromExplosion(Entity damagee, Entity damager) {
		boolean cancel = false;

		//SKIP?: once feet are safe from explosion, do same for eyes? no because this is for all entities which might not be 2 tall
		//	consider doing point to bounding box check (see https://gist.github.com/aadnk/7123926)

		//if (generated block between damagee and damager) cancel
		World world = damagee.getWorld();
		Point source = new Point(damager.getLocation()).add(0, 0.5, 0);
		Point target = new Point(damagee.getLocation()).add(0, 0.5, 0);
		Vector direction = target.toVector().subtract(source.toVector());
		int distance = Math.max(1, (int) source.distance(target));
		BlockIterator rayBlocks = new BlockIterator(world, source.toVector(), direction, 0, distance);
		while (rayBlocks.hasNext()) {
			Block rayBlock = rayBlocks.next();
			Point rayPoint = new Point(rayBlock);

			if (isGenerated(rayPoint)) {
				cancel = true;
//				Debug.msg("cancelled explosion damage due to generated rayPoint " + rayPoint);
//				Debug.particleAtTimed(rayPoint, ParticleEffect.HEART);
				break;
			} else {
//				Debug.particleAtTimed(rayPoint, ParticleEffect.FLAME);
			}
		}

		return cancel;
	}

	public boolean onEnderPearlThrown(Player player, Point source, Point target) {
		boolean cancel = false;

		//cancel pearl if source is generated (feet and eyes)
		World world = player.getWorld();
		if (player.isInsideVehicle()) {
			//player technically has eyes in vehicle but practically speaking player has feet in vehicle
			source = source.add(0, 1, 0);
		}
		Point feet = source;
		Point eyes = source.add(0, 1, 0);
		boolean feetGenerated = FortressesManager.forWorld(world).isGenerated(feet);
		boolean eyesGenerated = FortressesManager.forWorld(world).isGenerated(eyes);
		if (feetGenerated || eyesGenerated) {
			String msg = ChatColor.AQUA + "Pearling while inside a fortress wall is not allowed.";
			player.sendMessage(msg);
			cancel = true;

			//give back ender pearl
			player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
		}

		return cancel;
	}

	public void breakRune(GeneratorRune rune) {
		Set<Point> patternPoints = rune.getPattern().getPoints();

		rune.onBroken();
		//breaking should naturally update: generatedPoints and generatorRuneByProtectedPoint

		for (Point p : patternPoints) {
			model.generatorRuneByPatternPoint.remove(p);
		}

		model.generatorRunes.remove(rune);
	}
}