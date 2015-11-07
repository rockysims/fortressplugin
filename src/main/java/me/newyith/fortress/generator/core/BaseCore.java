package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Wall;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

public class BaseCore {
	private static class Model {
		private Point anchorPoint = null;
		private Set<Point> claimedPoints = null;
		private Set<Point> claimedWallPoints = null;
		private CoreAnimator animator = null;
		private UUID placedByPlayerId = null;
		private Set<Point> layerOutsideFortress = null;
		private Set<Point> pointsInsideFortress = null;
		private String worldName = null;
		private transient World world = null;
		private transient final int generationRangeLimit;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("claimedPoints") Set<Point> claimedPoints,
					 @JsonProperty("claimedWallPoints") Set<Point> claimedWallPoints,
					 @JsonProperty("animator") CoreAnimator animator,
					 @JsonProperty("placedByPlayerId") UUID placedByPlayerId,
					 @JsonProperty("layerOutsideFortress") Set<Point> layerOutsideFortress,
					 @JsonProperty("pointsInsideFortress") Set<Point> pointsInsideFortress,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.claimedPoints = claimedPoints;
			this.claimedWallPoints = claimedWallPoints;
			this.animator = animator;
			this.placedByPlayerId = placedByPlayerId;
			this.layerOutsideFortress = layerOutsideFortress;
			this.pointsInsideFortress = pointsInsideFortress;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.generationRangeLimit = FortressPlugin.config_generationRangeLimit;
			//"//updateInsideOutside() called by rune (second stage loading)" not sure if this is important
		}
	}
	private Model model = null;

	@JsonCreator
	public BaseCore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BaseCore(World world, Point anchorPoint) {
		Set<Point> claimedPoints = new HashSet<>();
		Set<Point> claimedWallPoints = new HashSet<>();
		CoreAnimator animator = new CoreAnimator(world, anchorPoint);
		UUID placedByPlayerId = null; //set by onCreated()
		Set<Point> layerOutsideFortress = new HashSet<>();
		Set<Point> pointsInsideFortress = new HashSet<>();
		String worldName = world.getName();
		model = new Model(anchorPoint, claimedPoints, claimedWallPoints, animator,
				placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
	}

	public void secondStageLoad() {
		//this is needed in case of /reload during generation
		model.animator.getWallMaterials().refresh(); //needs to be in second stage because refresh uses runeByPoint lookup

		/* rebuild version (currently saving it instead)
		updateClaimedPoints(claimedWallPoints); //needs to be in second stage because uses runeByPoint lookup
		//*/
	}

	//------------------------------------------------------------------------------------------------------------------

	public Player getOwner() {
		return Bukkit.getPlayer(model.placedByPlayerId);
	}

	public boolean playerCanOpenDoor(Player player, Point doorPoint) {
		String playerName = player.getName();
		Set<Point> potentialSigns = new HashSet<>();
		boolean isTrapDoor = Wall.isTrapDoor(doorPoint.getBlock(model.world).getType());

		if (isTrapDoor) {
			potentialSigns.addAll(Wall.getAdjacent6(doorPoint));
		} else {
			//potentialSigns.addAll(point above door and points adjacent to it)
			Point aboveDoorPoint = new Point(doorPoint).add(0, 1, 0);
			potentialSigns.addAll(Wall.getAdjacent6(aboveDoorPoint));
			potentialSigns.add(aboveDoorPoint);
		}

		if (signMustBeInside(doorPoint)) {
			potentialSigns.retainAll(model.pointsInsideFortress);
		}

		//fill actualSigns (from potentialSigns)
		Set<Point> actualSigns = new HashSet<>();
		for (Point potentialSign : potentialSigns) {
			Material mat = potentialSign.getBlock(model.world).getType();
			if (Wall.isSign(mat)) {
				actualSigns.add(potentialSign);
			}
		}

		//potentialSigns.addAll(connected signs)
		Point origin = doorPoint;
		Set<Point> originLayer = actualSigns;
		Set<Material> traverseMaterials = Wall.getSignMaterials();
		Set<Material> returnMaterials = Wall.getSignMaterials();
		int rangeLimit = model.generationRangeLimit * 2;
		Set<Point> ignorePoints = null;
		Set<Point> searchablePoints = null;
		Set<Point> connectedSigns = Wall.getPointsConnected(model.world, origin, originLayer,
				traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
		actualSigns.addAll(connectedSigns);

		Set<String> names = new HashSet<>();
		for (Point p : actualSigns) {
			if (Wall.isSign(p.getBlock(model.world).getType())) {
				Block signBlock = p.getBlock(model.world);
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
		boolean doorIsTrapDoor = Wall.isTrapDoor(doorPoint.getBlock(model.world).getType());
		if (!doorIsTrapDoor) {
			doorPoints.add(new Point(doorPoint).add(0, -1, 0));
		}
		for (Point p : doorPoints) {
			adjacentToDoor.addAll(Wall.getAdjacent6(p));
		}
		adjacentToDoor.removeAll(doorPoints);

		//if any of the blocks adjacent to door point(s) are pointsInsideFortress, signMustBeInside = true
		boolean allAdjacentAreOutside = Collections.disjoint(adjacentToDoor, model.pointsInsideFortress);
		signMustBeInside = !allAdjacentAreOutside;

		return signMustBeInside;
	}

	private Set<String> getNamesFromSign(Sign sign) {
		Set<String> names = new HashSet<>();

		if (sign != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(sign.getLine(0));
			sb.append("\n");
			sb.append(sign.getLine(1));
			sb.append("\n");
			sb.append(sign.getLine(2));
			sb.append("\n");
			sb.append(sign.getLine(3));
			String s = sb.toString().replaceAll(" ", "");

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

	public boolean onCreated(Player placingPlayer) {
		model.placedByPlayerId = placingPlayer.getUniqueId();

		//set overlapWithClaimed = true if placed generator is connected (by faces) to another generator's claimed points
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		Set<Point> claimPoints = rune.getPoints();
		Set<Point> alreadyClaimedPoints = getClaimedPointsOfNearbyGenerators();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			animator.wallMats.refresh(); //refresh protectable blocks list based on chest contents
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

	public Set<Point> getClaimedPoints() {
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

	public Set<Point> getAlteredPoints() {
		return animator.getAlteredPoints();
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
