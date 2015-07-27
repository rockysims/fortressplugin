package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

public class FortressGeneratorRune {
	private static ArrayList<FortressGeneratorRune> runeInstances = new ArrayList<FortressGeneratorRune>();

    private FortressGeneratorRunePattern pattern;

	public static void onPlayerRightClickBlock(Player player, Block clickedBlock) {
        FortressGeneratorRunePattern runePattern = new FortressGeneratorRunePattern(clickedBlock);
        if (runePattern.matchesReadyPattern()) {
			boolean runeAlreadyCreated = false;
			for (FortressGeneratorRune rune : runeInstances) {
				Point a = new Point(rune.pattern.getAnchorBlock().getLocation());
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
        }
	}

	public static void onBlockBreakEvent(Block brokenBlock) {
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}

	public static void onWaterBreaksRedstoneWireEvent(Block brokenBlock) {
		Bukkit.broadcastMessage("onWaterBreaksRedstoneWireEvent at " + new Point(brokenBlock.getLocation()));
		onRuneMightHaveBeenBrokenBy(brokenBlock);
	}

	public static void onBlockPlaceEvent(Block placedBlock) {
		onRuneMightHaveBeenBrokenBy(placedBlock);
	}

	public static void onRuneMightHaveBeenBrokenBy(Block block) {
		for (Iterator<FortressGeneratorRune> it = runeInstances.iterator(); it.hasNext();) {
			FortressGeneratorRune rune = it.next();

			Bukkit.broadcastMessage("onRuneMightHaveBeenBrokenBy checking rune at " + new Point(rune.pattern.getAnchorBlock().getLocation()));
			if (rune.pattern.contains(block)) {
				rune.onBroken();
				it.remove();
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------

    public FortressGeneratorRune(FortressGeneratorRunePattern runePattern) {
        this.pattern = runePattern;
    }

	public void onCreated() {
		Bukkit.broadcastMessage("Successfully created fortress generator rune.");
		this.pattern.setSignText("created");
	}

    public void onBroken() {
		Bukkit.broadcastMessage("FortressGeneratorRune onBroken() called");
		this.pattern.setSignText("broken");
		Bukkit.broadcastMessage("FortressGeneratorRune onBroken() sign updated");
		//runeInstances.remove(this);
		Bukkit.broadcastMessage("FortressGeneratorRune onBroken() done");
    }
}
