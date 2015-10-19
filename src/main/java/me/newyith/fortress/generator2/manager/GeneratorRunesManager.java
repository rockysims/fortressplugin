package me.newyith.fortress.generator2.manager;

import me.newyith.fortress.generator2.rune.GeneratorRune;
import me.newyith.fortress.generator2.rune.GeneratorRunePattern;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Wall;
import org.bukkit.ChatColor;
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

public class GeneratorRunesManager {
	private ArrayList<GeneratorRune> runes = new ArrayList<>();
	private HashMap<Point, GeneratorRune> runeByRunePoint = new HashMap<>();
	private HashMap<Point, GeneratorRune> runeByProtectedPoint = new HashMap<>();
	private Set<Point> protectedPoints = new HashSet<>();
	private Set<Point> alteredPoints = new HashSet<>();

	//------------------------------------------------------------------------------------------------------------------

	// - Getters / Setters -

	public GeneratorRune getRune(Point p) {
		return runeByRunePoint.get(p);
	}

	public List<GeneratorRune> getRunes() {
		return runes;
	}

	public Set<GeneratorRune> getOtherRunesInRange(Point center, int range) {
		//TODO: update this to use fortress cuboid instead of fixed range

		Set<GeneratorRune> runesInRange = new HashSet<>();
		int x = (int)center.x;
		int y = (int)center.y;
		int z = (int)center.z;

		//fill runesInRange
		for (GeneratorRune rune : runes) {
			//set inRange
			boolean inRange = true;
			Point p = rune.getAnchor();
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

	public void addProtectedPoint(Point p, Point anchor) {
		protectedPoints.add(p);
		runeByProtectedPoint.put(p, runeByRunePoint.get(anchor));
	}

	public void removeProtectedPoint(Point p) {
		protectedPoints.remove(p);
		runeByProtectedPoint.remove(p);
	}

	public void addAlteredPoint(Point p) {
		alteredPoints.add(p);
	}

	public void removeAlteredPoint(Point p) {
		alteredPoints.remove(p);
	}

	public boolean isGenerated(Point p) {
		return protectedPoints.contains(p) || alteredPoints.contains(p);
	}

	public boolean isClaimed(Point p) {
		boolean claimed = false;

		Iterator<GeneratorRune> it = runes.iterator();
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
		return runes.size();
	}

	// - Utils -

	private void createRune(GeneratorRunePattern runePattern, Player player) {
		GeneratorRune rune = new GeneratorRune(runePattern);
		runes.add(rune);

		//add new rune to runeByRunePoint map
		for (Point p : rune.getPattern().getPoints()) {
			runeByRunePoint.put(p, rune);
		}

		//nothing to add to runeByProtectedPoint because newly created rune can't already be generating anything

		rune.onCreated(player);
	}

	private void destroyRune(GeneratorRune rune) {
		List<Point> patternPoints = rune.getPattern().getPoints();

		rune.onBroken(); //breaking rune degenerates wall so runeByProtectedPoint should be cleaned up naturally

		for (Point p : patternPoints) {
			runeByRunePoint.remove(p);
		}

		runes.remove(rune);
	}

	// - Events -

	//consider creating rune
	public boolean onSignChange(Player player, Block placedBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryPatternAt(placedBlock);
		if (runePattern != null) {
			boolean runeAlreadyCreated = runeByRunePoint.containsKey(new Point(placedBlock.getLocation()));
			if (!runeAlreadyCreated) {
				createRune(runePattern, player);
				cancel = true; //otherwise initial text on sign is replaced by what user wrote
			} else {
				String msg = "Failed to create rune because rune already created here.";
				player.sendMessage(ChatColor.AQUA + msg);
			}
		}

		return cancel;
	}

	private void onRuneMightHaveBeenBrokenBy(Block block) {
		if (runeByRunePoint.containsKey(new Point(block.getLocation()))) {
			GeneratorRune rune = runeByRunePoint.get(new Point(block.getLocation()));
			destroyRune(rune);
		}
	}

	//consider cancelling if protected point (or part of protected piston)
	//consider destroying rune if in rune pattern
	public void onBlockBreakEvent(BlockBreakEvent event) {
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

	public void onWaterBreaksRedstoneWireEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}

	//consider cancelling if protected point (to protect water/lava sources)
	//consider destroying rune if in rune pattern
	public boolean onBlockPlaceEvent(Player player, Block placedBlock) {
		boolean cancel = false;

		Point placedPoint = new Point(placedBlock.getLocation());
		boolean isProtected = protectedPoints.contains(placedPoint);
		boolean inCreative = player.getGameMode() == GameMode.CREATIVE;
		if (isProtected && !inCreative) {
			cancel = true;
		}

		if (!cancel) {
			onRuneMightHaveBeenBrokenBy(placedBlock);
		}

		return cancel;
	}









	// - Events (more/old) -

	public void onTick() {
		runes.forEach(GeneratorRune::onTick);
	}

	//detect rune un/powered

	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		int signal = event.getNewCurrent();
		Block block = event.getBlock();
		Point p = new Point(block.getLocation());

		//if the redstone that changed is part of the rune, update rune state
		if (runeByRunePoint.containsKey(new Point(block.getLocation()))) {
			GeneratorRune rune = runeByRunePoint.get(new Point(block.getLocation()));
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

	public void onExplode(List<Block> explodeBlocks) {
		Iterator<Block> it = explodeBlocks.iterator();
		while (it.hasNext()) {
			Point p = new Point(it.next().getLocation());
			if (protectedPoints.contains(p)) {
				Debug.msg("explode removed at " + p);
				it.remove();
			}
		}
	}

	public void onPlayerOpenCloseDoor(PlayerInteractEvent event) {
		Block doorBlock = event.getClickedBlock();
		Point doorPoint = new Point(doorBlock.getLocation());

		GeneratorRune rune = runeByProtectedPoint.get(doorPoint);
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

	public boolean onPistonEvent(boolean isSticky, Point piston, Point target, ArrayList<Block> movedBlocks) {
		boolean cancel = false;

		if (movedBlocks != null) {
			for (Block movedBlock : movedBlocks) {
				Point movedPoint = new Point(movedBlock.getLocation());
				if (protectedPoints.contains(movedPoint)) {
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
					Point p = new Point(b.getLocation());
					pointsAffected.add(p);
				}
			}

			//build runesAffected
			HashSet<GeneratorRune> runesAffected = new HashSet<>();
			for (Point p : pointsAffected) {
				GeneratorRune rune = runeByRunePoint.get(p);
				if (rune != null) {
					runesAffected.add(rune);
				}
			}

			//break affected runes
			for (GeneratorRune rune : runesAffected) {
				destroyRune(rune);
			}

			//Debug.msg("piston: " + piston);
			//Debug.msg("target: " + target);
			//Debug.msg("target type: " + target.getBlock().getType());
			Debug.msg("movedBlocks.size(): " + movedBlocks.size());
		}

		return cancel;
	}
}
