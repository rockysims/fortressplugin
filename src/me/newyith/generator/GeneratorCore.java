package me.newyith.generator;

import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class GeneratorCore implements Memorable {
	//saved
	private GeneratorCoreAnimator animator = new GeneratorCoreAnimator();
	private Set<Point> claimedPoints = new HashSet<>();
	private Set<Point> claimedWallPoints = new HashSet<>();
	private Point anchorPoint = null; //set by constructor
	private UUID placedByPlayerId = null; //set by onPlaced

	//not saved
	private final int generationRangeLimit = 32;

	//------------------------------------------------------------------------------------------------------------------

	public void saveTo(Memory m) {
		m.save("animator", animator);

		m.save("claimedPoints", claimedPoints);
		Debug.msg("saved claimedPoints: " + claimedPoints.size());

		m.save("claimedWallPoints", claimedWallPoints);
		Debug.msg("saved claimedWallPoints: " + claimedWallPoints.size());

		m.save("anchorPoint", anchorPoint);
		Debug.msg("saved anchorPoint: " + anchorPoint);

		m.save("placedByPlayerIdString", placedByPlayerId.toString());
		//Debug.msg("saved placedByPlayerId: " + placedByPlayerId);
	}

	public static GeneratorCore loadFrom(Memory m) {
		GeneratorCoreAnimator animator = m.loadGenerationAnimator("animator");

		Set<Point> claimedPoints = m.loadPointSet("claimedPoints");
		Debug.msg("loaded claimedPoints: " + claimedPoints.size());

		Set<Point> claimedWallPoints = m.loadPointSet("claimedWallPoints");
		Debug.msg("loaded claimedWallPoints: " + claimedWallPoints.size());

		Point anchorPoint = m.loadPoint("anchorPoint");
		Debug.msg("loaded anchorPoint: " + anchorPoint);

		UUID placedByPlayerId = UUID.fromString(m.loadString("placedByPlayerIdString"));

		GeneratorCore instance = new GeneratorCore(
				animator,
				claimedPoints,
				claimedWallPoints,
				anchorPoint,
				placedByPlayerId);
		return instance;
	}

	private GeneratorCore(
			GeneratorCoreAnimator animator,
			Set<Point> claimedPoints,
			Set<Point> claimedWallPoints,
			Point anchorPoint,
			UUID placedByPlayerId) {
		this.animator = animator;
		this.claimedPoints = claimedPoints;
		this.claimedWallPoints = claimedWallPoints;
		this.anchorPoint = anchorPoint;
		this.placedByPlayerId = placedByPlayerId;
	}

	//------------------------------------------------------------------------------------------------------------------

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

	public GeneratorCore(Point anchorPoint) {
		this.anchorPoint = anchorPoint;
	}

	// - Events -

	public boolean onPlaced(Player placingPlayer) { //<--------- called by rune
		this.placedByPlayerId = placingPlayer.getUniqueId();

		//set overlapWithClaimed = true if placed generator is connected (by faces) to another generator's claimed points
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(this.anchorPoint);
		Set<Point> claimPoints = rune.getPoints();
		Set<Point> alreadyClaimedPoints = this.getClaimedPointsOfNearbyGenerators();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			//claim wall + 1 layer (and 1 layer around generator)
			List<List<Point>> generatableWallLayers = this.getGeneratableWallLayers();
			this.updateClaimedPoints(generatableWallLayers); //updateClaimedPoints() will add in layer around wall + generator and layer around it

			//tell player how many wall blocks were found
			int foundWallPointsCount = Wall.flattenLayers(generatableWallLayers).size();
			this.sendMessage("Fortress generator found " + String.valueOf(foundWallPointsCount) + " wall blocks.");
		} else {
			this.sendMessage("Fortress generator is too close to another generator's wall.");
		}

		return canPlace;
	}

	public void onBroken() { //<--------- called by rune
		this.degenerateWall(false);
	}

	public void onStateChanged(FgState newState) { //<--------- called by rune
		if (newState == FgState.RUNNING) {
			this.generateWall();
		} else {
			this.degenerateWall(true);
		}
	}

	public void tick() {
		animator.tick();
	}

	// --------- Internal Methods ---------

	private void sendMessage(String msg) {
		msg = ChatColor.AQUA + msg;
		Bukkit.getPlayer(this.placedByPlayerId).sendMessage(msg);
	}

	/**
	 * Degenerates (turns off) the wall being generated by this generator.
	 */
	private void degenerateWall(boolean animate) {
		Debug.msg("degenerateWall("+String.valueOf(animate)+")");
		getClaimedPointsOfNearbyGenerators(); //make nearby generators look for and degenerate any claimed but unconnected blocks
		animator.degenerate(animate);
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 * Clogs generator if called too often (more than once per second).
	 */
	private void generateWall() {
		Debug.msg("generateWall()");

		List<List<Point>> layers = this.getGeneratableWallLayers();
		this.updateClaimedPoints(Wall.merge(layers, animator.getGeneratedLayers()));

		animator.generate(layers);
	}

	private List<List<Point>> getGeneratableWallLayers() {
		Set<Point> claimedPoints = this.getClaimedPointsOfNearbyGenerators();

		//return all connected wall points ignoring (and not traversing) claimedPoints (generationRangeLimit search range)
		List<List<Point>> allowedWallLayers = getPointsConnectedAsLayers(Wall.getWallMaterials(), Wall.getGeneratableWallMaterials(), generationRangeLimit, claimedPoints);

		return allowedWallLayers;
	}

	private void updateClaimedPoints(List<List<Point>> wallLayers) {
		this.updateClaimedPoints(Wall.flattenLayers(wallLayers));
	}
	private void updateClaimedPoints(Set<Point> wallPoints) {
		this.claimedPoints.clear();

		//claim wallLayers
		this.claimedWallPoints = wallPoints;
		this.claimedPoints.addAll(this.claimedWallPoints);

		//claim layer around wall
		Set<Point> layerAroundWallPoints = getLayerAround(this.claimedWallPoints);
		this.claimedPoints.addAll(layerAroundWallPoints);

		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(this.anchorPoint);
		if (rune != null) {
			//claim rune
			Set<Point> runePoints = rune.getPoints();
			this.claimedPoints.addAll(runePoints);
			//claim layer around rune
			Set<Point> layerAroundRune = getLayerAround(runePoints);
			this.claimedPoints.addAll(layerAroundRune);
		}
	}

	private Set<Point> getClaimedPointsOfNearbyGenerators() {
		Set<FortressGeneratorRune> nearbyRunes = FortressGeneratorRunesManager.getOtherRunesInRange(this.anchorPoint, generationRangeLimit * 2 + 1); //not sure if the + 1 is needed

		Set<Point> claimedPoints = new HashSet<>();
		for (FortressGeneratorRune rune : nearbyRunes) {
			claimedPoints.addAll(rune.getGeneratorCore().getClaimedPoints());
		}

		return claimedPoints;
	}

	private Set<Point> getClaimedPoints() {
		//update claimedPoints if claimedWallPoints are not all wall type blocks
		for (Point p : this.claimedWallPoints) {
			Material claimedWallMaterial = p.getBlock().getType();
			if (!Wall.getWallMaterials().contains(claimedWallMaterial)) { //claimedWallMaterial isn't a wall type block
				this.unclaimDisconnected();
				break;
			}
		}

		return this.claimedPoints;
	}

	public Set<Point> getProtectedPoints() {
		return animator.getProtectedPoints();
	}

	private void unclaimDisconnected() {
		//fill pointsToUnclaim
		Set<Point> pointsToUnclaim = new HashSet<>();
		Set<Point> connectedPoints = getPointsConnected(Wall.getWallMaterials(), Wall.getWallMaterials(), generationRangeLimit, null, this.claimedWallPoints);
		for (Point claimedWallPoint : this.claimedWallPoints) {
			if (!connectedPoints.contains(claimedWallPoint)) { //found claimed wall point that is now disconnected
				pointsToUnclaim.add(claimedWallPoint);
			}
		}

		//unclaim pointsToUnclaim
		this.claimedWallPoints.removeAll(pointsToUnclaim);
		this.updateClaimedPoints(this.claimedWallPoints);

		//degenerate overlap between pointsToUnclaim and generatedLayers
		Set<Point> pointsToDegenerate = new HashSet<>(pointsToUnclaim);
		pointsToDegenerate.retainAll(Wall.flattenLayers(animator.getGeneratedLayers())); //not sure this line is really needed
		animator.degenerate(pointsToDegenerate);
	}

	private Set<Point> getPointsConnected(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(this.anchorPoint);
		Set<Point> originLayer = rune.getPoints();
		return Wall.getPointsConnected(this.anchorPoint, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
	}

	private List<List<Point>> getPointsConnectedAsLayers(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints) {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(this.anchorPoint);
		Set<Point> originLayer = rune.getPoints();
		return Wall.getPointsConnectedAsLayers(this.anchorPoint, originLayer, wallMaterials, returnMaterials, rangeLimit, ignorePoints);
	}

	private Set<Point> getLayerAround(Set<Point> wallPoints) {
		Set<Material> wallMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Wall.getPointsConnected(this.anchorPoint, wallPoints, wallMaterials, returnMaterials, rangeLimit, ignorePoints, Wall.ConnectedThreshold.POINTS);
	}



//	will be used for emergency key
//	public String getPlacedByPlayerId() {
//		return this.placedByPlayerId;
//	}

//	will be used for emergency key
//	private List<FortressGeneratorRune> getConnectedFortressGenerators() {
//		List<FortressGeneratorRune> matches = new ArrayList<>();
//
//		Set<Point> connectRunePoints = getPointsConnected(Wall.getWallMaterials(), Wall.getNotCloggedGeneratorBlocks());
//		for (Point p : connectRunePoints) {
//			FortressGeneratorRune fg = (FortressGeneratorRune) world.getTileEntity(p.x, p.y, p.z);
//			matches.add(fg);
//		}
//
//		return matches;
//	}



}
