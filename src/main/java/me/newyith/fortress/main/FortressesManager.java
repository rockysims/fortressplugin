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
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
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

	//--- fields (saved and transient) ---

	private Set<GeneratorRune> generatorRunes = new HashSet<>();
	private transient Map<Point, GeneratorRune> generatorRuneByPointMap = null;
	private transient Map<Point, GeneratorRune> generatorRuneByProtectedPointMap = null;
	private transient Set<Point> protectedPoints = null;
	private transient Set<Point> alteredPoints = null;

	@JsonProperty("generatorRunes")
	private void setGeneratorRunes(Set<GeneratorRune> generatorRunes) {
		Debug.msg("FortressesManager.setGeneratorRunes() called via @JsonProperty annotation. YAY!");
		this.generatorRunes = generatorRunes;
		buildTransients();
	}

	private void buildTransients() {
		generatorRuneByPointMap = new HashMap<>();
		generatorRuneByProtectedPointMap = new HashMap<>();
		protectedPoints = new HashSet<>();
		alteredPoints = new HashSet<>();

		//TODO: build
	}



	//--- utils (for keeping transients updated when changing saved fields) ---

	private void addGeneratorRune(GeneratorRune rune) {
		generatorRunes.add(rune);

		//update generatorRuneByPointMap
		for (Point p : rune.getPattern().getPoints()) {
			generatorRuneByPointMap.put(p, rune);
		}

		/* //TODO: uncomment this out once GeneratorRune has GeneratorCore
		//update alteredPoints
		Set<Point> altereds = rune.getGeneratorCore().getAlteredPoints();
		alteredPoints.addAll(altereds);

		//update protectedPoints
		Set<Point> protecteds = rune.getGeneratorCore().getProtectedPoints();
		protectedPoints.addAll(protecteds);

		//update runeByProtectedPoint
		for (Point p : protecteds) {
			generatorRuneByProtectedPointMap.put(p, rune);
		}
		//*/
	}
	private void removeGeneratorRune(GeneratorRune rune) {
		for (Point p : rune.getPattern().getPoints()) {
			generatorRuneByPointMap.remove(p);
		}

		//update generatorRuneByPointMap
		Set<Point> patternPoints = rune.getPattern().getPoints();
		for (Point p : patternPoints) {
			generatorRuneByPointMap.remove(p);
		}

		//generatorRuneByProtectedPointMap, alteredPoints, and protectedPoints should update naturally on rune destroyed

		generatorRunes.remove(rune);
	}

	//-----------------------------------------------------------------------

	public static boolean onSignChange(Player player, Block signBlock) {
		boolean cancel = false;

		GeneratorRunePattern runePattern = GeneratorRunePattern.tryReadyPattern(signBlock);
		if (runePattern != null) {
			boolean runeAlreadyCreated = instance.generatorRuneByPointMap.containsKey(new Point(signBlock));
			if (!runeAlreadyCreated) {
				GeneratorRune rune = new GeneratorRune(runePattern);
				instance.addGeneratorRune(rune);
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
