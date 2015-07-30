package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

public class FortressGeneratorRunesManager {
	private static ArrayList<FortressGeneratorRune> runeInstances = new ArrayList<FortressGeneratorRune>();

	public static void saveTo(Memory m) {
		m.save("runeInstances", runeInstances);
	}

	public static void loadFrom(Memory m) {
		runeInstances = m.loadFortressGeneratorRunes("runeInstances");
	}

	//------------------------------------------------------------------------------------------------------------------

	public static void onPlayerRightClickBlock(Player player, Block clickedBlock) {
		FortressGeneratorRunePattern runePattern = new FortressGeneratorRunePattern(clickedBlock);
		if (runePattern.matchedReadyPattern()) {
			boolean runeAlreadyCreated = false;
			for (FortressGeneratorRune rune : runeInstances) {
				Point a = rune.getPattern().anchorPoint;
				Point p = new Point(clickedBlock.getLocation());
				if (a.equals(p)) {
					runeAlreadyCreated = true;
				}
			}
			if (!runeAlreadyCreated) {
				FortressGeneratorRune rune = new FortressGeneratorRune(runePattern);
				runeInstances.add(rune);
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

	public static void onPotentialRedstoneEvent(Block block, int signal) {
		for (Iterator<FortressGeneratorRune> it = runeInstances.iterator(); it.hasNext();) {
			FortressGeneratorRune rune = it.next();

			Point a = rune.getPattern().anchorPoint;
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
		//TODO: rewrite this to make it more efficient (it gets called a LOT)
		for (Iterator<FortressGeneratorRune> it = runeInstances.iterator(); it.hasNext();) {
			FortressGeneratorRune rune = it.next();

			Bukkit.broadcastMessage("onRuneMightHaveBeenBrokenBy checking rune at " + rune.getPattern().anchorPoint);
			if (rune.getPattern().contains(block)) {
				rune.onBroken();
				it.remove();
			}
		}
	}
}
