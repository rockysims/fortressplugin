package me.newyith.fortress.main;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.generator.rune.GeneratorRunePattern;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
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
		private Set<GeneratorRune> generatorRunes = new HashSet<>();
		private transient Map<Point, GeneratorRune> generatorRuneByPoint = null;
		private transient Map<Point, GeneratorRune> generatorRuneByProtectedPoint = null;
		private transient Set<Point> protectedPoints = null;
		private transient Set<Point> alteredPoints = null;

		public Model() {
			refreshTransients();
		}

		private void onLoaded() {
			refreshTransients();
		}

		private void addGeneratorRune(GeneratorRune rune) {
			generatorRunes.add(rune);
			refreshTransients();
		}

		private void refreshTransients() {
			generatorRuneByPoint = new HashMap<>();
			generatorRuneByProtectedPoint = new HashMap<>();
			protectedPoints = new HashSet<>();
			alteredPoints = new HashSet<>();

			for (GeneratorRune rune : generatorRunes) {
				//rebuild runeByPoint map
				for (Point p : rune.getPattern().getPoints()) {
					generatorRuneByPoint.put(p, rune);
				}

				//TODO: build others

//				//rebuild alteredPoints
//				Set<Point> altereds = rune.getGeneratorCore().getAlteredPoints();
//				alteredPoints.addAll(altereds);
//
//				//rebuild protectedPoints
//				Set<Point> protecteds = rune.getGeneratorCore().getProtectedPoints();
//				protectedPoints.addAll(protecteds);
//
//				//rebuild runeByProtectedPoint
//				for (Point p : protecteds) {
//					runeByProtectedPoint.put(p, rune);
//				}
			}
		}
	}
	private Model model = new Model();

	@JsonProperty("model")
	private void setModel(Model model) {
		this.model = model;
		model.onLoaded();
	}

	//-----------------------------------------------------------------------








	//-----------------------------------------------------------------------

	public static boolean onSignChange(Player player, Block signBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryReadyPattern(signBlock);
		if (runePattern != null) {
			boolean runeAlreadyCreated = instance.model.generatorRuneByPoint.containsKey(new Point(signBlock));
			if (!runeAlreadyCreated) {
				GeneratorRune rune = new GeneratorRune(runePattern);
				instance.model.addGeneratorRune(rune);
				rune.onCreated(player);
				cancel = true; //otherwise initial text on sign is replaced by what user wrote
			} else {
				player.sendMessage(ChatColor.AQUA + "Failed to create rune because rune already created here.");
			}
		}

		Debug.msg("FortressesManager.onSignChange");
		return cancel;
	}

	public static void onBlockBreakEvent(BlockBreakEvent event) {
//		Block brokenBlock = event.getBlock();
//		Point brokenPoint = new Point(brokenBlock.getLocation());
//
//		boolean isProtected = protectedPoints.contains(brokenPoint);
//		boolean inCreative = event.getPlayer().getGameMode() == GameMode.CREATIVE;
//		boolean cancel = false;
//		if (isProtected && !inCreative) {
//			cancel = true;
//		} else {
//			if (brokenPoint.is(Material.PISTON_EXTENSION) || brokenPoint.is(Material.PISTON_MOVING_PIECE)) {
//				MaterialData matData = brokenBlock.getState().getData();
//				if (matData instanceof PistonExtensionMaterial) {
//					PistonExtensionMaterial pem = (PistonExtensionMaterial) matData;
//					BlockFace face = pem.getFacing().getOppositeFace();
//					Point pistonPoint = new Point(brokenBlock.getRelative(face, 1).getLocation());
//					if (protectedPoints.contains(pistonPoint)) {
//						cancel = true;
//					}
//				} else {
//					Debug.error("matData not instanceof PistonExtensionMaterial");
//				}
//			}
//		}
//
//		if (cancel) {
//			event.setCancelled(true);
//		} else {
//			onRuneMightHaveBeenBrokenBy(brokenBlock);
//		}
	}

	//=======================================================================




	public static void onBlockBreakEvent_old(BlockBreakEvent event) {
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
