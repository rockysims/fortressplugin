package me.newyith.generator;

import me.newyith.memory.Memory;
import me.newyith.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FortressGeneratorRunesManager {
	private static ArrayList<FortressGeneratorRune> runeInstances = new ArrayList<FortressGeneratorRune>();
	private static HashMap<Point, FortressGeneratorRune> runeByPoint = new HashMap<>();

	public static void saveTo(Memory m) {
		m.save("runeInstances", runeInstances);
	}

	public static void loadFrom(Memory m) {
		runeInstances = m.loadFortressGeneratorRunes("runeInstances");

		//rebuild runeByPoint map
		for (FortressGeneratorRune rune : runeInstances) {
			for (Point p : rune.getPattern().getPoints()) {
				runeByPoint.put(p, rune);
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------

	public static void onTick() {
		for (FortressGeneratorRune rune : runeInstances) {
			rune.onTick();
		}
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

				rune.onCreated();
			} else {
				player.sendMessage("Failed to create rune because rune already created here.");
			}

//			player.getInventory().addItem(new ItemStack(Material.GLOWING_REDSTONE_ORE));
//			player.getInventory().addItem(new ItemStack(Material.SPONGE));
			//player.getInventory().addItem(new ItemStack(Material.BURNING_FURNACE));
//			player.getInventory().addItem(new ItemStack(Material.FARMLAND));
		}
	}

	public static void onBlockRedstoneEvent(Block block, int signal) {
		if (runeByPoint.containsKey(new Point(block.getLocation()))) {
			FortressGeneratorRune rune = runeByPoint.get(new Point(block.getLocation()));
			rune.setPowered(signal > 0);
		}
	}

	public static void onBlockBreakEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
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

	public static void doBreakRune(FortressGeneratorRune rune) {
		rune.onBroken();

		for (Point p : rune.getPattern().getPoints()) {
			runeByPoint.remove(p);
		}

		runeInstances.remove(rune);
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
	}
}
