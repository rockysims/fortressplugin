package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

public class FortressGeneratorRunesManager {
	private static ArrayList<FortressGeneratorRune> runeInstances = new ArrayList<FortressGeneratorRune>();

	public static void onPlayerRightClickBlock(Player player, Block clickedBlock) {
		FortressGeneratorRunePattern runePattern = new FortressGeneratorRunePattern(clickedBlock);
		if (runePattern.matchesReadyPattern()) {
			boolean runeAlreadyCreated = false;
			for (FortressGeneratorRune rune : runeInstances) {
				Point a = new Point(rune.getPattern().getAnchorBlock().getLocation());
				Point p = new Point(clickedBlock.getLocation());
				if (a.equals(p)) {
					runeAlreadyCreated = true;
				}
			}
			if (!runeAlreadyCreated) {
				FortressGeneratorRune rune = new FortressGeneratorRune(runePattern);
				runeInstances.add(rune);
				rune.onCreated();
			}
		}
	}

	public static void onPotentialRedstoneEvent(Block block, int signal) {
		for (Iterator<FortressGeneratorRune> it = runeInstances.iterator(); it.hasNext();) {
			FortressGeneratorRune rune = it.next();

			Point a = new Point(rune.getPattern().getAnchorBlock().getLocation());
			Point b = new Point(block.getLocation());
			if (a.equals(b)) {
				if (rune.isPowered() && signal <= 0) {
					rune.setPowered(false);
				} else if (!rune.isPowered() && signal > 0) {
					rune.setPowered(true);
				}
			}
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
		for (Iterator<FortressGeneratorRune> it = runeInstances.iterator(); it.hasNext();) {
			FortressGeneratorRune rune = it.next();

			if (rune.getPattern().contains(block)) {
				rune.onBroken();
				it.remove();
			}
		}
	}
}
