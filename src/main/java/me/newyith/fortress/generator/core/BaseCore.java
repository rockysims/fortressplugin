package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.WallMaterials;
import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.generator.rune.GeneratorState;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Debug;
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
	public static class Model {
		protected Point anchorPoint = null;
		protected Set<Point> claimedPoints = null;
		protected Set<Point> claimedWallPoints = null;
		protected CoreAnimator animator = null;
		protected UUID placedByPlayerId = null;
		protected Set<Point> layerOutsideFortress = null;
		protected Set<Point> pointsInsideFortress = null;
		protected String worldName = null;
		protected transient World world = null;
		protected transient final int generationRangeLimit;

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

		public Model(Model m) {
			this(m.anchorPoint, m.claimedPoints, m.claimedWallPoints, m.animator, m.placedByPlayerId, m.layerOutsideFortress, m.pointsInsideFortress, m.worldName);
		}
	}
	protected Model model = null;

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
		model.animator.getWallMats().refresh(); //needs to be in second stage because refresh uses runeByPoint lookup

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
		Set<Point> claimPoints = rune.getPattern().getPoints();
		Set<Point> alreadyClaimedPoints = getClaimedPointsOfNearbyGenerators();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			model.animator.getWallMats().refresh(); //refresh protectable blocks list based on chest contents
			Set<Point> generatableWallPoints = Wall.flattenLayers(getGeneratableWallLayers());
			updateInsideOutside(generatableWallPoints);
			//claim wall + 1 layer (and 1 layer around generator)
			updateClaimedPoints(generatableWallPoints); //updateClaimedPoints() will add in layer around wall + generator and layer around it

			//tell player how many wall blocks were found
			int foundWallPointsCount = generatableWallPoints.size();
			sendMessage("Fortress generator found " + foundWallPointsCount + " wall blocks.");
		} else {
			sendMessage("Fortress generator is too close to another generator's wall.");
		}

		return canPlace;
	}

	public void onBroken() {
		degenerateWall(true); //true means skipAnimation
	}

	public void onStateChanged(GeneratorState newState) {
		if (newState == GeneratorState.RUNNING) {
			generateWall();
		} else {
			degenerateWall(false); //false means don't skipAnimation
		}
	}

	public void tick() {
		model.animator.tick();
	}

	// --------- Internal Methods ---------

	private void sendMessage(String msg) {
		msg = ChatColor.AQUA + msg;
		Bukkit.getPlayer(model.placedByPlayerId).sendMessage(msg);
	}

	/**
	 * Degenerates (turns off) the wall being generated by this generator.
	 */
	private void degenerateWall(boolean skipAnimation) {
		Debug.msg("degenerateWall(" + String.valueOf(skipAnimation) + ")");
		//getClaimedPointsOfNearbyGenerators(); //make nearby generators look for and degenerate any claimed but unconnected blocks
		model.animator.degenerate(skipAnimation);
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 */
	private void generateWall() {
		Debug.msg("generateWall()");
		model.animator.getWallMats().refresh(); //refresh protectable blocks list based on chest contents

		//TODO: make getGeneratableWallLayers() return promise?
		List<Set<Point>> generatableLayers = getGeneratableWallLayers();
		List<Set<Point>> wallLayers = Wall.merge(generatableLayers, model.animator.getGeneratedLayers());
		Set<Point> wallPoints = Wall.flattenLayers(wallLayers);
		updateClaimedPoints(wallPoints);
		updateInsideOutside(wallPoints);

		model.animator.generate(generatableLayers);
	}

	//TODO: delete this method (later) if nothing calls it
	public void updateInsideOutside() {
		updateInsideOutside(model.claimedWallPoints);
	}

	private void updateInsideOutside(Set<Point> wallPoints) {
		model.layerOutsideFortress.clear();
		model.pointsInsideFortress.clear();

		if (wallPoints.size() > 0) {
			Set<Point> layerAroundWall = getLayerAround(wallPoints);

			//find a top block in layerAroundWall
			Point top = layerAroundWall.iterator().next();
			for (Point p : layerAroundWall) {
				if (p.y() > top.y()) {
					top = p;
				}
			}

			//fill layerOutsideFortress
			Point origin = top;
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			Set<Material> traverseMaterials = null; //traverse all block types
			Set<Material> returnMaterials = null; //return all block types
			int rangeLimit = 2 * model.generationRangeLimit + 2;
			Set<Point> ignorePoints = wallPoints;
			Set<Point> searchablePoints = new HashSet<>(layerAroundWall);
			GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
			searchablePoints.addAll(rune.getPattern().getPoints());
			model.layerOutsideFortress = Wall.getPointsConnected(model.world, origin, originLayer,
					traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
			model.layerOutsideFortress.addAll(originLayer);
//			model.layerOutsideFortress.retainAll(layerAroundWall); //TODO: delete this line (pretty sure it's not needed)
			//Debug.msg("layerOutsideFortress.size(): " + layerOutsideFortress.size());

			//get layerInsideFortress
			Set<Point> layerInsideFortress = new HashSet<>(layerAroundWall);
			layerInsideFortress.removeAll(model.layerOutsideFortress);

			//fill pointsInsideFortress
			if (layerInsideFortress.size() > 0) {
				origin = layerInsideFortress.iterator().next();
				originLayer = layerInsideFortress;
				traverseMaterials = null; //traverse all block types
				returnMaterials = null; //all block types
				rangeLimit = 2 * model.generationRangeLimit;
				ignorePoints = wallPoints;
				searchablePoints = null; //search all points
				model.pointsInsideFortress = Wall.getPointsConnected(model.world, origin, originLayer,
						traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
				model.pointsInsideFortress.addAll(originLayer);
			}
		}
	}

	private List<Set<Point>> getGeneratableWallLayers() {
		Set<Point> claimedPoints = getClaimedPointsOfNearbyGenerators();

		//return all connected wall points ignoring (and not traversing) claimedPoints (generationRangeLimit search range)
		WallMaterials wallMats = model.animator.getWallMats();
		Set<Material> traverseMaterials = wallMats.getWallMaterials();
		Set<Material> returnMaterials = wallMats.getGeneratableWallMaterials();
		int rangeLimit = model.generationRangeLimit;
		Set<Point> ignorePoints = claimedPoints;
		List<Set<Point>> allowedWallLayers = getPointsConnectedAsLayers(traverseMaterials, returnMaterials, rangeLimit, ignorePoints);

		return allowedWallLayers;
	}

	private void updateClaimedPoints(Set<Point> wallPoints) {
		model.claimedPoints.clear();

		//claim wallLayers
		model.claimedWallPoints = wallPoints;
		model.claimedPoints.addAll(model.claimedWallPoints);

		//claim layer around wall
		Set<Point> layerAroundWallPoints = getLayerAround(model.claimedWallPoints);
		model.claimedPoints.addAll(layerAroundWallPoints);

		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		if (rune != null) {
			//claim rune
			Set<Point> runePoints = rune.getPattern().getPoints();
			model.claimedPoints.addAll(runePoints);
			//claim layer around rune
			Set<Point> layerAroundRune = getLayerAround(runePoints);
			model.claimedPoints.addAll(layerAroundRune);
		}
	}

	private Set<Point> getClaimedPointsOfNearbyGenerators() {
		Set<GeneratorRune> nearbyRunes = FortressesManager.getOtherGeneratorRunesInRange(model.anchorPoint, model.generationRangeLimit * 2 + 1); //not sure if the + 1 is needed

		Set<Point> claimedPoints = new HashSet<>();
		for (GeneratorRune rune : nearbyRunes) {
			claimedPoints.addAll(rune.getGeneratorCore().getClaimedPoints());
		}

		return claimedPoints;
	}

	public Set<Point> getClaimedPoints() {
		//commented out because now that protectable blocks can be changed by user
		//we don't want a generator to be able to degenerate another generator's generated points
		//even though the other generator's points are no longer connected to the generator by protectable block types
		/*
		//update claimedPoints if claimedWallPoints are not all wall type blocks
		for (Point p : model.claimedWallPoints) {
			Material claimedWallMaterial = p.getBlock(model.world).getType();
			if (!model.animator.getWallMats().getWallMaterials().contains(claimedWallMaterial)) { //claimedWallMaterial isn't a wall type block
				unclaimDisconnected();
				break;
			}
		}
		//*/

		return model.claimedPoints;
	}

	public Set<Point> getAlteredPoints() {
		return model.animator.getAlteredPoints();
	}

	public Set<Point> getProtectedPoints() {
		return model.animator.getProtectedPoints();
	}

	public Set<Point> getGeneratedPoints() {
		return model.animator.getGeneratedPoints();
	}

	public Set<Point> getLayerOutsideFortress() {
		return model.layerOutsideFortress;
	}

	private void unclaimDisconnected() {
		//fill pointsToUnclaim
		Set<Point> pointsToUnclaim = new HashSet<>();
		Set<Material> wallMaterials = model.animator.getWallMats().getWallMaterials();
		Set<Point> connectedPoints = getPointsConnected(wallMaterials, wallMaterials, model.generationRangeLimit, null, model.claimedWallPoints);
		for (Point claimedWallPoint : model.claimedWallPoints) {
			if (!connectedPoints.contains(claimedWallPoint)) { //found claimed wall point that is now disconnected
				pointsToUnclaim.add(claimedWallPoint);
			}
		}

		//unclaim pointsToUnclaim
		model.claimedWallPoints.removeAll(pointsToUnclaim);
		updateClaimedPoints(model.claimedWallPoints);

		//degenerate overlap between pointsToUnclaim and generatedLayers
		Set<Point> pointsToDegenerate = new HashSet<>(pointsToUnclaim);
		pointsToDegenerate.retainAll(Wall.flattenLayers(model.animator.getGeneratedLayers()));
		model.animator.degenerate(pointsToDegenerate);
	}

	private Set<Point> getPointsConnected(Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		Point origin = model.anchorPoint;
		Set<Point> originLayer = rune.getPattern().getPoints();
		return Wall.getPointsConnected(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
	}

	private List<Set<Point>> getPointsConnectedAsLayers(Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints) {
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		Point origin = model.anchorPoint;
		Set<Point> originLayer = rune.getPattern().getPoints();
		return Wall.getPointsConnectedAsLayers(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints);
	}

	private Set<Point> getLayerAround(Set<Point> originLayer) {
		Point origin = model.anchorPoint;
		Set<Material> traverseMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = model.generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Wall.getPointsConnected(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, Wall.ConnectedThreshold.POINTS);
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
