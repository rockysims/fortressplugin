package me.newyith.generator;

import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import me.newyith.util.Wall;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.*;

import java.util.*;

public class FortressGeneratorRunesManager {
	private static ArrayList<FortressGeneratorRune> runeInstances = new ArrayList<>();
	private static HashMap<Point, FortressGeneratorRune> runeByPoint = new HashMap<>();
	private static HashMap<Point, FortressGeneratorRune> runeByProtectedPoint = new HashMap<>();
	private static Set<Point> protectedPoints = new HashSet<>();

	public static void saveTo(Memory m) {
		m.save("runeInstances", runeInstances);
	}

	public static void loadFrom(Memory m) {
		runeInstances = m.loadFortressGeneratorRunes("runeInstances");

		for (FortressGeneratorRune rune : runeInstances) {
			//rebuild runeByPoint map
			for (Point p : rune.getPattern().getPoints()) {
				runeByPoint.put(p, rune);
			}

			Set<Point> protecteds = rune.getGeneratorCore().getProtectedPoints();

			//rebuild protectedPoints
			protectedPoints.addAll(protecteds);

			//rebuild runeByProtectedPoint
			for (Point p : protecteds) {
				runeByProtectedPoint.put(p, rune);
			}
		}

		//second stage loading
		for (FortressGeneratorRune rune : runeInstances) {
			rune.secondStageLoad();
		}
	}

	//------------------------------------------------------------------------------------------------------------------

	// - Getters / Setters -

	public static FortressGeneratorRune getRune(Point p) {
		return runeByPoint.get(p);
	}

	public static Set<FortressGeneratorRune> getOtherRunesInRange(Point center, int range) {
		Set<FortressGeneratorRune> runesInRange = new HashSet<>();
		int x = (int)center.x;
		int y = (int)center.y;
		int z = (int)center.z;

		//fill runesInRange
		for (FortressGeneratorRune rune : runeInstances) {
			//set inRange
			boolean inRange = true;
			Point p = rune.getPattern().anchorPoint;
			inRange = inRange && Math.abs(p.x - x) <= range;
			inRange = inRange && Math.abs(p.y - y) <= range;
			inRange = inRange && Math.abs(p.z - z) <= range;

			if (inRange) {
				runesInRange.add(rune);
			}
		}

		runesInRange.remove(getRune(center));
		return runesInRange;
	}

	public static void addProtectedPoint(Point p, Point anchor) {
		protectedPoints.add(p);
		runeByProtectedPoint.put(p, runeByPoint.get(anchor));
	}

	public static void removeProtectedPoint(Point p) {
		protectedPoints.remove(p);
		runeByProtectedPoint.remove(p);
	}

	public static int getRuneCount() {
		return runeInstances.size();
	}

	// - Events -

	public static void onTick() {
		runeInstances.forEach(FortressGeneratorRune::onTick);
	}

	public static void onPlayerRightClickBlock(Player player, Block clickedBlock) {
		FortressGeneratorRunePattern runePattern = new FortressGeneratorRunePattern(clickedBlock);
		if (runePattern.matchedReadyPattern()) {
			boolean runeAlreadyCreated = runeByPoint.containsKey(new Point(clickedBlock.getLocation()));
			if (!runeAlreadyCreated) {
				FortressGeneratorRune rune = new FortressGeneratorRune(runePattern);
				runeInstances.add(rune);

				//add new rune to runeByPoint map
				for (Point p : runePattern.getPoints()) {
					runeByPoint.put(p, rune);
				}

				rune.onCreated(player);
			} else {
				player.sendMessage("Failed to create rune because rune already created here.");
			}

//			player.getInventory().addItem(new ItemStack(Material.GLOWING_REDSTONE_ORE));
//			player.getInventory().addItem(new ItemStack(Material.SPONGE));
			//player.getInventory().addItem(new ItemStack(Material.BURNING_FURNACE));
//			player.getInventory().addItem(new ItemStack(Material.FARMLAND));
		}
	}

	public static void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		int signal = event.getNewCurrent();
		Block block = event.getBlock();
		Point p = new Point(block.getLocation());

		//if the redstone that changed is part of the rune, update rune state
		if (runeByPoint.containsKey(new Point(block.getLocation()))) {
			FortressGeneratorRune rune = runeByPoint.get(new Point(block.getLocation()));
			rune.setPowered(signal > 0);
		}

		//if door is protected, ignore redstone event
		if (Wall.isDoor(block.getType()) && protectedPoints.contains(p)) {
			Openable openableDoor = (Openable)block.getState().getData();
			if (openableDoor.isOpen()) {
				event.setNewCurrent(1);
			} else {
				event.setNewCurrent(0);
			}
		}
	}

	public static void onExplode(List<Block> explodeBlocks) {
		Iterator<Block> it = explodeBlocks.iterator();
		while (it.hasNext()) {
			Point p = new Point(it.next().getLocation());
			if (protectedPoints.contains(p)) {
				Debug.msg("explode removed at " + p);
				it.remove();
			}
		}
	}

	public static void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Block doorBlock = event.getClickedBlock();
		Point doorPoint = new Point(doorBlock.getLocation());

		FortressGeneratorRune rune = runeByProtectedPoint.get(doorPoint);
		if (rune != null) {
			Player player = event.getPlayer();
			Point aboveDoorPoint = new Point(doorPoint).add(0, 1, 0);
			switch (aboveDoorPoint.getBlock().getType()) {
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
				Material doorType = doorPoint.getBlock().getType();
				boolean isIronDoor = doorType == Material.IRON_DOOR_BLOCK;
				boolean isIronTrap = doorType == Material.IRON_TRAPDOOR;
				if (isIronDoor || isIronTrap) {
					BlockState state = doorBlock.getState();
					boolean nowOpen;
					if (isIronDoor) {
						Door door = (Door) state.getData();
						if (door.isTopHalf()) {
							Block bottomDoorBlock = (new Point(doorPoint).add(0, -1, 0)).getBlock();
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
						player.playSound(doorPoint.toLocation(), Sound.DOOR_OPEN, 1.0F, 1.0F);
					} else {
						player.playSound(doorPoint.toLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
					}
				}
			}
		}
	}

	public static void onBlockBreakEvent(BlockBreakEvent event) {
		Block brokenBlock = event.getBlock();
		Point brokenPoint = new Point(brokenBlock.getLocation());

		boolean isProtected = protectedPoints.contains(brokenPoint);
		boolean inCreative = event.getPlayer().getGameMode() == GameMode.CREATIVE;
		boolean cancel = false;
		if (isProtected && !inCreative) {
			cancel = true;
		} else {
			if (brokenPoint.is(Material.PISTON_EXTENSION) || brokenPoint.is(Material.PISTON_MOVING_PIECE)) {
				MaterialData matData = brokenBlock.getState().getData();
				if (matData instanceof PistonExtensionMaterial) {
					PistonExtensionMaterial pem = (PistonExtensionMaterial) matData;
					BlockFace face = pem.getFacing().getOppositeFace();
					Point pistonPoint = new Point(brokenBlock.getRelative(face, 1).getLocation());
					if (protectedPoints.contains(pistonPoint)) {
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
	public static void onWaterBreaksRedstoneWireEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}
	public static void onBlockPlaceEvent(Block placedBlock) {
		onRuneMightHaveBeenBrokenBy(placedBlock);
	}
	private static void onRuneMightHaveBeenBrokenBy(Block block) {
		if (runeByPoint.containsKey(new Point(block.getLocation()))) {
			FortressGeneratorRune rune = runeByPoint.get(new Point(block.getLocation()));

			if (rune.getPattern().contains(block)) {
				doBreakRune(rune);
			}
		}
	}

	public static void onPistonEvent(boolean isSticky, Point piston, Point target, ArrayList<Block> movedBlocks) {
		//build pointsAffected
		HashSet<Point> pointsAffected = new HashSet<>();
		pointsAffected.add(piston);
		if (target != null) {
			pointsAffected.add(target);
		}
		if (movedBlocks != null) {
			for (Block b : movedBlocks) {
				Point p = new Point(b.getLocation());
				pointsAffected.add(p);
			}
		}

		//build runesAffected
		HashSet<FortressGeneratorRune> runesAffected = new HashSet<>();
		for (Point p : pointsAffected) {
			FortressGeneratorRune rune = runeByPoint.get(p);
			if (rune != null) {
				runesAffected.add(rune);
			}
		}

		for (FortressGeneratorRune rune : runesAffected) {
			doBreakRune(rune);
		}




		Debug.msg("piston: " + piston);
		Debug.msg("target: " + target);
		//Debug.msg("target type: " + target.getBlock().getType());
		Debug.msg("movedBlocks.size(): " + movedBlocks.size());








	}

	public static void doBreakRune(FortressGeneratorRune rune) {
		List<Point> patternPoints = rune.getPattern().getPoints();

		rune.onBroken();

		for (Point p : patternPoints) {
			runeByPoint.remove(p);
		}

		runeInstances.remove(rune);
	}
}
