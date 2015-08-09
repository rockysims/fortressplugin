package me.newyith.generator;

import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class GeneratorCore implements Memorable {
	//saved
	private HashMap<Point, Material> alteredPoints = new HashMap<>();
	private Set<Point> protectedPoints = new HashSet<>();
	private Set<Point> claimedPoints = new HashSet<>();
	private Set<Point> claimedWallPoints = new HashSet<>();
	private List<List<Point>> generatedLayers = new ArrayList<>();
	private List<List<Point>> animationWallLayers = new ArrayList<>();
	private boolean animateGeneration = true;
	private boolean isChangingGenerated = false;
	private boolean isGeneratingWall = false;
	private Point anchorPoint = null; //set by constructor
	private UUID placedByPlayerId = null; //set by onPlaced

	//not saved
	private long lastFrameTimestamp = 0;
	private final long msPerFrame = 150;
	private final int generationRangeLimit = 32;

	//------------------------------------------------------------------------------------------------------------------

	public void saveTo(Memory m) {
		Debug.msg("saving alteredPoints: " + alteredPoints.size());
		m.save("alteredPoints", alteredPoints);
		Debug.msg("saved alteredPoints: " + alteredPoints.size());

		m.save("protectedPoints", protectedPoints);
		Debug.msg("saved protectedPoints: " + protectedPoints.size());

		m.save("claimedPoints", claimedPoints);
		Debug.msg("saved claimedPoints: " + claimedPoints.size());

		m.save("claimedWallPoints", claimedWallPoints);
		Debug.msg("saved claimedWallPoints: " + claimedWallPoints.size());

		m.save("generatedLayers", generatedLayers);
		Debug.msg("saved generatedLayers: " + generatedLayers.size());

		m.save("animationWallLayers", animationWallLayers);
		Debug.msg("saved animationWallLayers: " + animationWallLayers.size());

		m.save("animateGeneration", animateGeneration);
		Debug.msg("saved animateGeneration: " + animateGeneration);

		m.save("isChangingGenerated", isChangingGenerated);
		Debug.msg("saved isChangingGenerated: " + isChangingGenerated);

		m.save("isGeneratingWall", isGeneratingWall);
		Debug.msg("saved isGeneratingWall: " + isGeneratingWall);

		m.save("anchorPoint", anchorPoint);
		Debug.msg("saved anchorPoint: " + anchorPoint);

		m.save("placedByPlayerIdString", placedByPlayerId.toString());
		//Debug.msg("saved placedByPlayerId: " + placedByPlayerId);
	}

	public static GeneratorCore loadFrom(Memory m) {
		HashMap<Point, Material> alteredPoints = m.loadPointMaterialMap("alteredPoints");
		Debug.msg("loaded alteredPoints: " + alteredPoints.size());

		Set<Point> protectedPoints = m.loadPointSet("protectedPoints");
		Debug.msg("loaded protectedPoints: " + protectedPoints.size());

		Set<Point> claimedPoints = m.loadPointSet("claimedPoints");
//		claimedPoints = new HashSet<>(); //TODO: delete this line
		Debug.msg("loaded claimedPoints: " + claimedPoints.size());

		Set<Point> claimedWallPoints = m.loadPointSet("claimedWallPoints");
//		claimedWallPoints = new HashSet<>(); //TODO: delete this line
		Debug.msg("loaded claimedWallPoints: " + claimedWallPoints.size());

		List<List<Point>> generatedLayers = m.loadLayers("generatedLayers");
		Debug.msg("loaded generatedLayers: " + generatedLayers.size());

		List<List<Point>> animationWallLayers = m.loadLayers("animationWallLayers");
		Debug.msg("loaded animationWallLayers: " + animationWallLayers.size());

		boolean animateGeneration = m.loadBoolean("animateGeneration");
		Debug.msg("loaded animateGeneration: " + animateGeneration);

		boolean isChangingGenerated = m.loadBoolean("isChangingGenerated");
		Debug.msg("loaded isChangingGenerated: " + isChangingGenerated);

		boolean isGeneratingWall = m.loadBoolean("isGeneratingWall");
		Debug.msg("loaded isGeneratingWall: " + isGeneratingWall);

		Point anchorPoint = m.loadPoint("anchorPoint");
		Debug.msg("loaded anchorPoint: " + anchorPoint);

		UUID placedByPlayerId = UUID.fromString(m.loadString("placedByPlayerIdString"));

		GeneratorCore instance = new GeneratorCore(alteredPoints,
				protectedPoints,
				claimedPoints,
				claimedWallPoints,
				generatedLayers,
				animationWallLayers,
				animateGeneration,
				isChangingGenerated,
				isGeneratingWall,
				anchorPoint,
				placedByPlayerId);
		return instance;
	}

	private GeneratorCore(HashMap<Point, Material> alteredPoints,
						  Set<Point> protectedPoints,
						  Set<Point> claimedPoints,
						  Set<Point> claimedWallPoints,
						  List<List<Point>> generatedLayers,
						  List<List<Point>> animationWallLayers,
						  boolean animateGeneration,
						  boolean isChangingGenerated,
						  boolean isGeneratingWall,
						  Point anchorPoint,
						  UUID placedByPlayerId) {
		this.alteredPoints = alteredPoints;
		this.protectedPoints = protectedPoints;
		this.claimedPoints = claimedPoints;
		this.claimedWallPoints = claimedWallPoints;
		this.generatedLayers = generatedLayers;
		this.animationWallLayers = animationWallLayers;
		this.animateGeneration = animateGeneration;
		this.isChangingGenerated = isChangingGenerated;
		this.isGeneratingWall = isGeneratingWall;
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
		claimPoints.addAll(getLayerAround(claimPoints));
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

	public void sendMessage(String msg) {
		msg = ChatColor.AQUA + msg;
		Bukkit.getPlayer(this.placedByPlayerId).sendMessage(msg);
	}

	public void tick() {
		if (this.isChangingGenerated) {
			long now = (new Date()).getTime();
			//if (ready to update to next frame)
			if (!this.animateGeneration  || now - this.lastFrameTimestamp >= this.msPerFrame ) {
				this.lastFrameTimestamp  = now;

				//update to next frame
				boolean noNextFrame = !this.updateToNextFrame();
				if (noNextFrame) {
					this.isChangingGenerated = false;
				}

				//if (not animating) we finished all at once
				if (!this.animateGeneration) {
					this.isChangingGenerated = false;
				}
			}
		}
	}

	// --------- Internal Methods ---------

	//TODO: refactor animation handling to new class GeneratorCoreAnimation?

	private boolean updateToNextFrame() {
		boolean foundLayerToUpdate = false;

		//TODO: consider saving i as animationLayerIndex for increased execution speed
		for (int i = 0; i < this.animationWallLayers.size(); i++) {
			int layerIndex = i;
			//if (degenerating) start from the outer most layer
			if (!this.isGeneratingWall) {
				layerIndex = (animationWallLayers.size()-1) - i;
			}

			List<Point> layer = new ArrayList<>(this.animationWallLayers.get(layerIndex)); //make copy to avoid concurrent modification errors (recheck this is needed)

			//try to update layer
			foundLayerToUpdate = updateLayer(layer, layerIndex);
			if (foundLayerToUpdate && this.animateGeneration) {
				//updated a layer so we're done with this frame
				break;
			}
		} // end for (List<Point> layer : this.wallPoints)


		return foundLayerToUpdate;
	}

	private boolean updateLayer(List<Point> layer, int layerIndex) {
		boolean updatedLayer = false;

		for (Point p : layer) {
			if (this.isGeneratingWall) {
				//try to generate block at p
				boolean pGenerated = alter(p) || protect(p);
				updatedLayer = updatedLayer || pGenerated;

				if (pGenerated) {
					//add p to generatedLayers
					while (layerIndex >= this.generatedLayers.size()) {
						this.generatedLayers.add(new ArrayList<>());
					}
					this.generatedLayers.get(layerIndex).add(p);
				}
			} else {
				//try to degenerate block at p
				boolean pDegenerated = unalter(p) || unprotect(p);
				updatedLayer = updatedLayer || pDegenerated;

				if (pDegenerated) {
					//remove p from generatedLayers
					if (layerIndex < this.generatedLayers.size()) {
						this.generatedLayers.get(layerIndex).remove(p);
					} //else we would be degenerating another generators wall
				}
			}
		} // end for (Point p : layer)

		return updatedLayer;
	}

	private boolean alter(Point p) {
		boolean altered = false;

		Block b = p.getBlock();
		if (Wall.isAlterableWallMaterial(b.getType())) {
			this.alteredPoints.put(p, b.getType());
			b.setType(Material.BEDROCK);
			altered = true;
		}



		//TODO: delete this block of code
		if (!altered) {
			//Debug.msg("failed to alter at " + p);
		}



		return altered;
	}

	private boolean unalter(Point p) {
		boolean unaltered = false;

		if (this.alteredPoints.containsKey(p)) {
			Material material = this.alteredPoints.remove(p);
			if (p.getBlock().getType() == Material.BEDROCK) {
				p.getBlock().setType(material);
			}
			unaltered = true;
		}

		return unaltered;
	}

	private boolean protect(Point p) {
		boolean pointProtected = false;

		Block b = p.getBlock();
		if (!this.protectedPoints.contains(p) && Wall.isProtectableWallMaterial(b.getType())) {
			this.protectedPoints.add(p);
			//TODO: make FortressGeneratorParticlesManager show particles on protectedPoints
			//TODO: make block at p unbreakable
			pointProtected = true;
		}

		return pointProtected;
	}

	private boolean unprotect(Point p) {
		boolean unprotected = false;

		if (this.protectedPoints.contains(p)) {
			this.protectedPoints.remove(p);
			//TODO: make block at p breakable again
			unprotected = true;
		}

		return unprotected;
	}

	/**
	 * Degenerates (turns off) the wall being generated by this generator.
	 */
	private void degenerateWall(boolean animate) {
		Debug.msg("degenerateWall("+String.valueOf(animate)+")");

		this.animationWallLayers.clear();
		this.animationWallLayers.addAll(this.generatedLayers);

		this.isGeneratingWall = false;
		this.isChangingGenerated = true;

		if (!animate) {
			this.animateGeneration = false;
			this.tick();
			this.animateGeneration = true;
		}
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 * Clogs generator if called too often (more than once per second).
	 */
	private void generateWall() {
		Debug.msg("generateWall()");

		//set this.wallLayers = wall layers its allowed to generate
		this.animationWallLayers = this.getGeneratableWallLayers();
		//recalculate this.claimedPoints
		this.updateClaimedPoints(Wall.merge(this.animationWallLayers, this.generatedLayers));

		//start generating
		this.isGeneratingWall = true;
		this.isChangingGenerated = true;
	}

	// --------- More Internal Methods ---------

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

		//degenerate overlap between pointsToUnclaim and this.generatedLayers
		Set<Point> pointsToDegenerate = new HashSet<>(pointsToUnclaim);
		pointsToDegenerate.retainAll(Wall.flattenLayers(this.generatedLayers));
		this.animationWallLayers.clear();
		Debug.msg("unclaimDisconnected() this.animationWallLayers.clear()");
		this.animationWallLayers.add(new ArrayList<>(pointsToDegenerate));
		this.isGeneratingWall = false;
		this.isChangingGenerated = true;
		this.animateGeneration = false;
		this.tick();
		this.animateGeneration = true;

		//remove pointsToDegenerate from this.generatedLayers
		for (Iterator<List<Point>> itr = this.generatedLayers.iterator(); itr.hasNext(); ) {
			List<Point> layer = itr.next();
			layer.removeAll(pointsToDegenerate);
			if (layer.size() == 0) {
				itr.remove();
			}
		}
		this.isGeneratingWall = this.generatedLayers.size() > 0;
	}

	private Set<Point> getPointsConnected(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		return Wall.getPointsConnected(this.anchorPoint, wallMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
	}

	private List<List<Point>> getPointsConnectedAsLayers(Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints) {
		return Wall.getPointsConnectedAsLayers(this.anchorPoint, wallMaterials, returnMaterials, rangeLimit, ignorePoints);
	}

	private Set<Point> getLayerAround(Set<Point> wallPoints) {
		Set<Material> wallMaterials = new HashSet<>(); //no wall blocks
		Set<Material> returnMaterials = null; //all blocks are return blocks
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
