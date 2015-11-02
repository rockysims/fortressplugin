package me.newyith.fortress.main;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.generator.rune.GeneratorRunePattern;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.model.GeneratorRuneSet;
import me.newyith.fortress.util.Point;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.*;

public class FortressesManager { //implements Modelable statically
	public static class Model extends BaseModel {
		//generatorRunes
		private transient GeneratorRuneSet generatorRunes = null;
		private GeneratorRuneSet.Model generatorRunesModel = null;
		public GeneratorRuneSet getGeneratorRunes() {
			if (generatorRunes == null) {
				generatorRunes = new GeneratorRuneSet(generatorRunesModel);
			}
			return generatorRunes;
		}

		//runeByPoint (not saved)
		private transient HashMap<Point, GeneratorRune> runeByPoint = null;
		public Map<Point, GeneratorRune> getRuneByPointMap() {
			if (runeByPoint == null) {
				runeByPoint = new HashMap<>();
				Iterator<GeneratorRune> it = generatorRunes.iterator();



			}
			return runeByPoint;
		}

		public Model(GeneratorRuneSet generatorRunes) {
			this.generatorRunesModel = generatorRunes.getModel();
		}
	}
	private static Model model = null;

	public static void setModel(Model m) {
		model = m;
	}

	public static Model getModel() {
		if (model == null) {
			model = new Model(new GeneratorRuneSet());
		}
		return model;
	}

	//-----------------------------------------------------------------------







	public static boolean onSignChange_old(Player player, Block placedBlock) {
		boolean cancel = false;

		FortressGeneratorRunePattern runePattern = FortressGeneratorRunePattern.tryPatternAt(placedBlock);
		if (runePattern != null) {
			boolean runeAlreadyCreated = runeByPoint.containsKey(new Point(placedBlock.getLocation()));
			if (!runeAlreadyCreated) {
				FortressGeneratorRune rune = new FortressGeneratorRune(runePattern);
				runeInstances.add(rune);

				//add new rune to runeByPoint map
				for (Point p : runePattern.getPoints()) {
					runeByPoint.put(p, rune);
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

	//TODO: write this method
	public static boolean onSignChange(Player player, Block signBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryReadyPattern(signBlock);
		if (runePattern != null) {

		}


		Debug.msg("FortressesManager.onSignChange");
		return cancel;
	}




	public static void onBlockBreakEvent(BlockBreakEvent event) {
		//TODO: write this method
		Debug.msg("FortressesManager.onBlockBreakEvent");
	}

	public static void onWaterBreaksRedstoneWireEvent(Block toBlock) {
		//TODO: write this method
		Debug.msg("FortressesManager.onWaterBreaksRedstoneWireEvent");
	}

	public static void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		//TODO: write this method
		Debug.msg("FortressesManager.onBlockRedstoneEvent");
	}




	public static void onTick() {

	}






	public static Set<GeneratorRune> getGeneratorRunesNear(Point center) {
		//TODO (later): return generator runes where cuboid overlaps with center
		return null;
	}

	//TODO: finish writing this method
	//	when getting nearby generators during generation, we need all potentially conflicting generators (not just known ones)
	//		so we need this method to get nearby by range
	//	when getting nearby generators during /fort stuck, we need all generators player might be inside of
	//		so also write getGeneratorRunesNear()
	public static Set<GeneratorRune> getGeneratorRunesInRange(Point center, int range) {
		Set<GeneratorRune> runesInRange = new HashSet<>();
//		int x = center.xInt();
//		int y = center.yInt();
//		int z = center.zInt();
//
//		//fill runesInRange
//		for (GeneratorRune rune : generatorRunes) {
//
//
//			//set inRange
//			boolean inRange = true;
//			Point p = rune.getPattern().anchorPoint;
//			inRange = inRange && Math.abs(p.xInt() - x) <= range;
//			inRange = inRange && Math.abs(p.yInt() - y) <= range;
//			inRange = inRange && Math.abs(p.zInt() - z) <= range;
//
//			if (inRange) {
//				runesInRange.add(rune);
//			}
//		}
//
//		runesInRange.remove(getRune(center));
		return runesInRange;
	}
	/*
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

	 */







}
