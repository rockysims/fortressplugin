package me.newyith.generator;

import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import me.newyith.util.Wall;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.*;

public class GeneratorCore implements Memorable {
	//saved
	private Set<Point> claimedPoints = new HashSet<>();
	private Set<Point> claimedWallPoints = new HashSet<>();
	private Point anchorPoint = null; //set by constructor
	private GeneratorCoreAnimator animator = null; //set by constructor
	private UUID placedByPlayerId = null; //set by onPlaced
	private Set<Point> layerOutsideFortress = new HashSet<>();
	private Set<Point> pointsInsideFortress = new HashSet<>(); //TODO: consider changing to layerInsideFortress

	//not saved
	private final int generationRangeLimit = 32;

	/*
	altered:
		blocks changed to bedrock
	protected:
		blocks made unbreakable
	generated:
		blocks made unbreakable and blocks changed to bedrock
	claimed:
		points the generate thinks it owns
	//*/

	//------------------------------------------------------------------------------------------------------------------

	public void saveTo(Memory m) {
		m.savePointSetCompact("claimedWallPoints", claimedWallPoints);
		Debug.msg("saved claimedWallPoints: " + claimedWallPoints.size());

		m.save("anchorPoint", anchorPoint);
		Debug.msg("saved anchorPoint: " + anchorPoint);

		m.save("animator", animator);

		m.save("placedByPlayerIdString", placedByPlayerId.toString());
		//Debug.msg("saved placedByPlayerId: " + placedByPlayerId);
	}

	public static GeneratorCore loadFrom(Memory m) {
		Set<Point> claimedWallPoints = m.loadPointSetCompact("claimedWallPoints");
		Debug.msg("loaded claimedWallPoints: " + claimedWallPoints.size());

		Point anchorPoint = m.loadPoint("anchorPoint");
		Debug.msg("loaded anchorPoint: " + anchorPoint);

		GeneratorCoreAnimator animator = m.loadGenerationAnimator("animator");

		UUID placedByPlayerId = UUID.fromString(m.loadString("placedByPlayerIdString"));

		//updateInsideOutside() called by runes manager (second stage loading)

		GeneratorCore instance = new GeneratorCore(
				animator,
				claimedWallPoints,
				anchorPoint,
				placedByPlayerId);
		return instance;
	}

	private GeneratorCore(
			GeneratorCoreAnimator animator,
			Set<Point> claimedWallPoints,
			Point anchorPoint,
			UUID placedByPlayerId) {
		this.animator = animator;
		this.claimedWallPoints = claimedWallPoints;
		this.anchorPoint = anchorPoint;
		this.placedByPlayerId = placedByPlayerId;
	}

	public void secondStageLoad() {
		//this is needed in case of /reload during generation
		animator.wallMats.refresh(); //needs to be in second stage because refresh uses runeByPoint lookup

		updateClaimedPoints(claimedWallPoints); //needs to be in second stage because uses runeByPoint lookup
	}

	//------------------------------------------------------------------------------------------------------------------

	public GeneratorCore(Point anchorPoint) {
		this.anchorPoint = anchorPoint;
		this.animator = new GeneratorCoreAnimator(anchorPoint);
	}

	public boolean playerCanOpenDoor(Player player, Point doorPoint) {
		String playerName = player.getName();
		Set<Point> points = new HashSet<>();

		//points.addAll(point above door and points adjacent to it)
		Point aboveDoorPoint = new Point(doorPoint).add(0, 1, 0);
		points.addAll(Wall.getAdjacent6(aboveDoorPoint));
		points.add(aboveDoorPoint);

		//points.addAll(point below door and points adjacent to it)
		Point belowDoorPoint = new Point(doorPoint).add(0, -1, 0);
		if (belowDoorPoint.getBlock().getType() == doorPoint.getBlock().getType()) {
			belowDoorPoint.y--;
		}
		points.addAll(Wall.getAdjacent6(belowDoorPoint));
		points.add(belowDoorPoint);

		if (signMustBeInside(doorPoint)) {
			points.retainAll(pointsInsideFortress);
		}

		//points.addAll(connected signs)
		Point origin = points.iterator().next();
		Set<Point> originLayer = points;
		Set<Material> wallMaterials = Wall.getSignMaterials();
		Set<Material> returnMaterials = Wall.getSignMaterials();
		int rangeLimit = generationRangeLimit * 2;
		Set<Point> ignorePoints = null;
		Set<Point> searchablePoints = null;
		Set<Point> connectedSigns = Wall.getPointsConnected(origin, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
		points.addAll(connectedSigns);

		Set<String> names = new HashSet<>();
		for (Point p : points) {
			if (Wall.isSign(p.getBlock().getType())) {
				Block signBlock = p.getBlock();
				Sign sign = (Sign)signBlock.getState();
				names.addAll(getNamesFromSign(sign));
			}
		}

		return names.contains(playerName);
	}

	private boolean signMustBeInside(Point doorPoint) {
		boolean signMustBeInside = false;

		//fill adjacentToDoor
		Set<Point> adjacentToDoor = new HashSet<>();
		Set<Point> doorPoints = new HashSet<>();
		doorPoints.add(doorPoint);
		Point belowDoorPoint = new Point(doorPoint).add(0, -1, 0);
		if (belowDoorPoint.getBlock().getType() == doorPoint.getBlock().getType()) {
			doorPoints.add(belowDoorPoint);
		}
		for (Point p : doorPoints) {
			adjacentToDoor.addAll(Wall.getAdjacent6(p));
		}

		//if any of the blocks adjacent to door point(s) are pointsInsideFortress, signMustBeInside = true
		for (Point p : adjacentToDoor) {
			if (pointsInsideFortress.contains(p)) {
				signMustBeInside = true;
				break;
			}
		}

		return signMustBeInside;
	}

	private Set<String> getNamesFromSign(Sign sign) {
		Set<String> names = new HashSet<>();

		if (sign != null) {
			String s = "";
			s += sign.getLine(0);
			s += "\n";
			s += sign.getLine(1);
			s += "\n";
			s += sign.getLine(2);
			s += "\n";
			s += sign.getLine(3);
			s = s.replaceAll(" ", "");

			if (s.contains(",")) {
				s = s.replaceAll("\n", "");
				for (String name : s.split(",")) {
					names.add(name);
				}
			} else {
				for (String name : s.split("\n")) {
					names.add(name);
				}
			}
		}

		return names;
	}

	// - Events -

	public boolean onPlaced(Player placingPlayer) { //<--------- called by rune
		placedByPlayerId = placingPlayer.getUniqueId();

		//set overlapWithClaimed = true if placed generator is connected (by faces) to another generator's claimed points
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		Set<Point> claimPoints = rune.getPoints();
		Set<Point> alreadyClaimedPoints = getClaimedPointsOfNearbyGenerators();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			Set<Point> generatableWallPoints = Wall.flattenLayers(getGeneratableWallLayers());
			updateInsideOutside(generatableWallPoints);
			//claim wall + 1 layer (and 1 layer around generator)
			updateClaimedPoints(generatableWallPoints); //updateClaimedPoints() will add in layer around wall + generator and layer around it

			//tell player how many wall blocks were found
			int foundWallPointsCount = generatableWallPoints.size();
			sendMessage("Fortress generator found " + String.valueOf(foundWallPointsCount) + " wall blocks.");
		} else {
			sendMessage("Fortress generator is too close to another generator's wall.");
		}

		return canPlace;
	}

	public void onBroken() { //<--------- called by rune
		degenerateWall(false);
	}

	public void onStateChanged(FgState newState) { //<--------- called by rune
		if (newState == FgState.RUNNING) {
			generateWall();
		} else {
			degenerateWall(true);
		}
	}

	public void tick() {
		animator.tick();
	}

	// --------- Internal Methods ---------

	private void sendMessage(String msg) {
		msg = ChatColor.AQUA + msg;
		Bukkit.getPlayer(placedByPlayerId).sendMessage(msg);
	}

	/**
	 * Degenerates (turns off) the wall being generated by this generator.
	 */
	private void degenerateWall(boolean animate) {
		Debug.msg("degenerateWall(" + String.valueOf(animate) + ")");
		//getClaimedPointsOfNearbyGenerators(); //make nearby generators look for and degenerate any claimed but unconnected blocks
		animator.degenerate(animate);
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 * Clogs generator if called too often (more than once per second).
	 */
	private void generateWall() {
		Debug.msg("generateWall()");
		animator.wallMats.refresh(); //refresh protectable blocks list based on chest contents

		List<List<Point>> generatableLayers = getGeneratableWallLayers();
		List<List<Point>> wallLayers = Wall.merge(generatableLayers, animator.getGeneratedLayers());
		Set<Point> wallPoints = Wall.flattenLayers(wallLayers);
		updateClaimedPoints(wallPoints);
		updateInsideOutside(wallPoints);

		animator.generate(generatableLayers);
	}

	public void updateInsideOutside() {
		updateInsideOutside(claimedWallPoints);
	}

	private void updateInsideOutside(Set<Point> wallPoints) {
		layerOutsideFortress.clear();
		pointsInsideFortress.clear();

		if (wallPoints.size() > 0) {
			Set<Point> layerAroundWall = getLayerAround(wallPoints);

			//find a top block in layerAroundWall
			Point top = layerAroundWall.iterator().next();
			for (Point p : layerAroundWall) {
				if (p.y > top.y) {
					top = p;
				}
			}

			//fill layerOutsideFortress
			Point origin = top;
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			Set<Material> wallMaterials = null; //traverse all block types
			Set<Material> returnMaterials = null; //return all block types
			int rangeLimit = 2 * generationRangeLimit + 2;
			Set<Point> ignorePoints = wallPoints;
			Set<Point> searchablePoints = new HashSet<>(layerAroundWall);
			FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
			searchablePoints.addAll(rune.getPoints());
			layerOutsideFortress = Wall.getPointsConnected(origin, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
			layerOutsideFortress.addAll(originLayer);
			layerOutsideFortress.retainAll(layerAroundWall);
			//Debug.msg("layerOutsideFortress.size(): " + layerOutsideFortress.size());


			//get layerInsideFortress
			Set<Point> layerInsideFortress = new HashSet<>(layerAroundWall);
			layerInsideFortress.removeAll(layerOutsideFortress);

			//fill pointsInsideFortress
			if (layerInsideFortress.size() > 0) {
				origin = layerInsideFortress.iterator().next();
				originLayer = layerInsideFortress;
				wallMaterials = null; //traverse all block types
				returnMaterials = null; //all block types
				rangeLimit = 2 * generationRangeLimit;
				ignorePoints = wallPoints;
				searchablePoints = null; //search all points
				pointsInsideFortress = Wall.getPointsConnected(origin, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
				pointsInsideFortress.addAll(originLayer);
			}
		}
	}

	private List<List<Point>> getGeneratableWallLayers() {
		Set<Point> claimedPoints = getClaimedPointsOfNearbyGenerators();

		//return all connected wall points ignoring (and not traversing) claimedPoints (generationRangeLimit search range)
		List<List<Point>> allowedWallLayers = getPointsConnectedAsLayers(animator.wallMats.getWallMaterials(), animator.wallMats.getGeneratableWallMaterials(), generationRangeLimit, claimedPoints);

		return allowedWallLayers;
	}

	private void updateClaimedPoints(Set<Point> wallPoints) {
		claimedPoints.clear();

		//claim wallLayers
		claimedWallPoints = wallPoints;
		claimedPoints.addAll(claimedWallPoints);

		//claim layer around wall
		Set<Point> layerAroundWallPoints = getLayerAround(claimedWallPoints);
		claimedPoints.addAll(layerAroundWallPoints);

		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		if (rune != null) {
			//claim rune
			Set<Point> runePoints = rune.getPoints();
			claimedPoints.addAll(runePoints);
			//claim layer around rune
			Set<Point> layerAroundRune = getLayerAround(runePoints);
			claimedPoints.addAll(layerAroundRune);
		}
	}

	private Set<Point> getClaimedPointsOfNearbyGenerators() {
		Set<FortressGeneratorRune> nearbyRunes = FortressGeneratorRunesManager.getOtherRunesInRange(anchorPoint, generationRangeLimit * 2 + 1); //not sure if the + 1 is needed

		Set<Point> claimedPoints = new HashSet<>();
		for (FortressGeneratorRune rune : nearbyRunes) {
			claimedPoints.addAll(rune.getGeneratorCore().getClaimedPoints());
		}

		return claimedPoints;
	}

	private Set<Point> getClaimedPoints() {
		//commented out because now that protectable blocks can be changed by user
		//we don't want a generator to be able to degenerate another generator's generated points
		//even though the other generator's points are no longer connected to the generator
		/*
		//update claimedPoints if claimedWallPoints are not all wall type blocks
		for (Point p : claimedWallPoints) {
			Material claimedWallMaterial = p.getBlock().getType();
			if (!animator.wallMats.getWallMaterials().contains(claimedWallMaterial)) { //claimedWallMaterial isn't a wall type block
				unclaimDisconnected();
				break;
			}
		}
		//*/

		return claimedPoints;
	}

	public Set<Point> getProtectedPoints() {
		return animator.getProtectedPoints();
	}

	public Set<Point> getGeneratedPoints() {
		return animator.getGeneratedPoints();
	}

	public Set<Point> getLayerOutsideFortress() {
		return this.layerOutsideFortress;
	}

	private void unclaimDisconnected() {
		//fill pointsToUnclaim
		Set<Point> pointsToUnclaim = new HashSet<>();
		Set<Point> connectedPoints = getPointsConnected(animator.wallMats.getWallMaterials(), animator.wallMats.getWallMaterials(), generationRangeLimit, null, claimedWallPoints);
		for (Point claimedWallPoint : claimedWallPoints) {
			if (!connectedPoints.contains(claimedWallPoint)) { //found claimed wall point that is now disconnected
				pointsToUnclaim.add(claimedWallPoint);
			}
		}

		//unclaim pointsToUnclaim
		claimedWallPoints.removeAll(pointsToUnclaim);
		updateClaimedPoints(claimedWallPoints);

		//degenerate overlap between pointsToUnclaim and generatedLayers
		Set<Point> pointsToDegenerate = new HashSet<>(pointsToUnclaim);
		pointsToDegenerate.retainAll(Wall.flattenLayers(animator.getGeneratedLayers())); //not sure this line is really needed
		animator.degenerate(pointsToDegenerate);
	}

	private Set<Point> getPointsConnected(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		Set<Point> originLayer = rune.getPoints();
		return Wall.getPointsConnected(anchorPoint, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
	}

	private List<List<Point>> getPointsConnectedAsLayers(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints) {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		Set<Point> originLayer = rune.getPoints();
		return Wall.getPointsConnectedAsLayers(anchorPoint, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints);
	}

	private Set<Point> getLayerAround(Set<Point> layerPoints) {
		Set<Material> wallMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Wall.getPointsConnected(anchorPoint, layerPoints, wallMaterials, returnMaterials, rangeLimit, ignorePoints, Wall.ConnectedThreshold.POINTS);
	}



//	will be used for emergency key
//	public String getPlacedByPlayerId() {
//		return placedByPlayerId;
//	}

//	will be used for emergency key
//	private List<FortressGeneratorRune> getConnectedFortressGenerators() {
//		List<FortressGeneratorRune> matches = new ArrayList<>();
//
//		Set<Point> connectRunePoints = getPointsConnected(animator.wallMats.getWallMaterials(), animator.wallMats.getNotCloggedGeneratorBlocks());
//		for (Point p : connectRunePoints) {
//			FortressGeneratorRune fg = (FortressGeneratorRune) world.getTileEntity(p.x, p.y, p.z);
//			matches.add(fg);
//		}
//
//		return matches;
//	}



}
