package me.newyith.fortress.core;

import me.newyith.fortress.core.util.GenPrepData;
import me.newyith.fortress.main.BedrockSafety;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Cuboid;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Blocks;
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
import java.util.concurrent.CompletableFuture;

public class BaseCore {
	public static class Model {
		protected final Point anchorPoint;
		protected final Set<Point> claimedPoints;
		protected final Set<Point> claimedWallPoints;
		protected final CoreAnimator animator;
		protected UUID placedByPlayerId;
		protected final Set<Point> layerOutsideFortress;
		protected final Set<Point> pointsInsideFortress;
		protected final String worldName;
		protected final transient World world;
		protected final transient int generationRangeLimit;
		protected transient CoreParticles coreParticles;
		protected transient CompletableFuture<GenPrepData> genPrepDataFuture;

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
			this.coreParticles = new CoreParticles();
			this.genPrepDataFuture = null;
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

	public BaseCore(World world, Point anchorPoint, CoreMaterials coreMats) {
		Set<Point> claimedPoints = new HashSet<>();
		Set<Point> claimedWallPoints = new HashSet<>();
		CoreAnimator animator = new CoreAnimator(world, anchorPoint, coreMats);
		UUID placedByPlayerId = null; //set by onCreated()
		Set<Point> layerOutsideFortress = new HashSet<>();
		Set<Point> pointsInsideFortress = new HashSet<>();
		String worldName = world.getName();
		model = new Model(anchorPoint, claimedPoints, claimedWallPoints, animator,
				placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public Player getOwner() {
		return Bukkit.getPlayer(model.placedByPlayerId);
	}

	public boolean playerCanOpenDoor(Player player, Point doorPoint) {
		String playerName = player.getName();
		Set<Point> actualSigns = getDoorWhitelistSignPoints(doorPoint);
		if (actualSigns.isEmpty()) {
			actualSigns = getFallbackWhitelistSignPoints();
		}

		Set<String> names = new HashSet<>();
		for (Point p : actualSigns) {
			if (Blocks.isSign(p.getBlock(model.world).getType())) {
				Block signBlock = p.getBlock(model.world);
				Sign sign = (Sign)signBlock.getState();
				names.addAll(getNamesFromSign(sign));
			}
		}

		return names.contains(playerName);
	}

	protected Set<Point> getFallbackWhitelistSignPoints() {
		//this method exists so GeneratorCore can override it
		return new HashSet<>();
	}

	private Set<Point> getDoorWhitelistSignPoints(Point doorPoint) {
		Set<Point> potentialSigns = new HashSet<>();
		boolean isTrapDoor = Blocks.isTrapDoor(doorPoint.getBlock(model.world).getType());

		if (isTrapDoor) {
			potentialSigns.addAll(Blocks.getAdjacent6(doorPoint));
		} else {
			//potentialSigns.addAll(point above door and points adjacent to it)
			Point aboveDoorPoint = new Point(doorPoint).add(0, 1, 0);
			potentialSigns.addAll(Blocks.getAdjacent6(aboveDoorPoint));
			potentialSigns.add(aboveDoorPoint);
		}

		if (signMustBeInside(doorPoint)) {
			potentialSigns.retainAll(model.pointsInsideFortress);
		}

		//fill actualSigns (from potentialSigns)
		Set<Point> actualSigns = new HashSet<>();
		for (Point potentialSign : potentialSigns) {
			Material mat = potentialSign.getBlock(model.world).getType();
			if (Blocks.isSign(mat)) {
				actualSigns.add(potentialSign);
			}
		}

		//potentialSigns.addAll(connected signs)
		Point origin = doorPoint;
		Set<Point> originLayer = actualSigns;
		Set<Material> traverseMaterials = Blocks.getSignMaterials();
		Set<Material> returnMaterials = Blocks.getSignMaterials();
		int rangeLimit = model.generationRangeLimit * 2;
		Set<Point> ignorePoints = null;
		Set<Point> searchablePoints = null;
		Set<Point> connectedSigns = Blocks.getPointsConnected(model.world, origin, originLayer,
				traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();
		actualSigns.addAll(connectedSigns);

		return actualSigns;
	}

	private boolean signMustBeInside(Point doorPoint) {
		boolean signMustBeInside = false;

		//fill adjacentToDoor
		Set<Point> adjacentToDoor = new HashSet<>();
		Set<Point> doorPoints = new HashSet<>();
		doorPoints.add(doorPoint);
		boolean doorIsTrapDoor = Blocks.isTrapDoor(doorPoint.getBlock(model.world).getType());
		if (!doorIsTrapDoor) {
			doorPoints.add(new Point(doorPoint).add(0, -1, 0));
		}
		for (Point p : doorPoints) {
			adjacentToDoor.addAll(Blocks.getAdjacent6(p));
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
		Set<Point> claimPoints = getOriginPoints();
		Set<Point> alreadyClaimedPoints = getClaimedPointsOfNearbyCores();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			getGenPrepDataFuture().thenAccept((data) -> {
				//update claimed points
				List<Set<Point>> wallLayers = Blocks.merge(data.generatableLayers, model.animator.getGeneratedLayers());
				Set<Point> wallPoints = Blocks.flattenLayers(wallLayers);
				updateClaimedPoints(wallPoints, data.layerAroundWall);

				//update inside & outside
				model.pointsInsideFortress.clear();
				model.pointsInsideFortress.addAll(data.pointsInside);
				model.layerOutsideFortress.clear();
				model.layerOutsideFortress.addAll(data.layerOutside);

				//tell player how many wall blocks were found
				sendMessage("Fortress generator found " + wallPoints.size() + " wall blocks.");
			});
		} else {
			sendMessage("Fortress generator is too close to another generator's wall.");
		}

		return canPlace;
	}

	public void onBroken() {
		degenerateWall(true); //true means skipAnimation
	}

	public void setActive(boolean active) {
		if (active) {
			generateWall();
		} else {
			degenerateWall(false); //false means don't skipAnimation
		}
	}

	public void onGeneratedChanged() {
		model.coreParticles.onGeneratedChanges();
	}

	public void tick() {
		CompletableFuture<GenPrepData> future = model.genPrepDataFuture;
		if (future != null) {
			GenPrepData data = future.getNow(null);
			if (data != null) {
				model.genPrepDataFuture = null;

				//* Generate

				//update claimed points
				List<Set<Point>> wallLayers = Blocks.merge(data.generatableLayers, model.animator.getGeneratedLayers());
				Set<Point> wallPoints = Blocks.flattenLayers(wallLayers);
				updateClaimedPoints(wallPoints, data.layerAroundWall);

				//update inside & outside
				model.pointsInsideFortress.clear();
				model.pointsInsideFortress.addAll(data.pointsInside);
				model.layerOutsideFortress.clear();
				model.layerOutsideFortress.addAll(data.layerOutside);

				//bedrock safety
				BedrockSafety.record(model.world, wallPoints);

				model.animator.generate(wallLayers);

				//*/
			} else {
				model.coreParticles.displayAnchorParticle(this);
			}
		}

		model.coreParticles.tick(this);

		boolean waitingForGenPrepData = future != null && !future.isDone();
		if (!waitingForGenPrepData) {
			model.animator.tick();
		}
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
//		Debug.msg("degenerateWall(" + String.valueOf(skipAnimation) + ")");

		//cancel pending generation (if any)
		if (model.genPrepDataFuture != null) {
			model.genPrepDataFuture.cancel(true); //apparently cancelling CompletableFuture doesn't actually stop process. it just doesn't resolve
			model.genPrepDataFuture = null;
		}

		//getClaimedPointsOfNearbyGenerators(); //make nearby generators look for and degenerate any claimed but unconnected blocks
		model.animator.degenerate(skipAnimation);
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 */
	private void generateWall() {
//		Debug.msg("generateWall()");

		//cancel pending generation (if any)
		if (model.genPrepDataFuture != null) {
			model.genPrepDataFuture.cancel(true); //apparently cancelling CompletableFuture doesn't actually stop process. it just doesn't resolve
			model.genPrepDataFuture = null;
		}

		//start preparing for generation (tick() handles it when future completes)
		model.genPrepDataFuture = getGenPrepDataFuture();
	}

	private CompletableFuture<GenPrepData> getGenPrepDataFuture() {
//		Debug.msg("getGenPrepDataFuture() called");

		CompletableFuture<GenPrepData> future = CompletableFuture.supplyAsync(() -> {
//			Debug.msg("getGenPrepDataFuture() start");
			List<Set<Point>> generatableLayers = getGeneratableWallLayers();

			//set layerAroundWall
			List<Set<Point>> wallLayers = Blocks.merge(generatableLayers, model.animator.getGeneratedLayers());
			Set<Point> wallPoints = Blocks.flattenLayers(wallLayers);
			Set<Point> layerAroundWall = getLayerAround(wallPoints, Blocks.ConnectedThreshold.POINTS).join();

			Set<Point> layerOutside = getLayerOutside(wallPoints, layerAroundWall);
			Set<Point> pointsInside = getPointsInside(layerOutside, layerAroundWall, wallPoints);

//			Debug.msg("getGenPrepDataFuture() returning");
			return new GenPrepData(generatableLayers, layerAroundWall, pointsInside, layerOutside);
		});

		onSearchingChanged(true);
		future.thenAccept((data) -> onSearchingChanged(false)); //thenAccept is not called if future was cancelled

		return future;
	}

	protected void onSearchingChanged(boolean searching) {
		//this method exists so GeneratorCore can override it
	}

	private Set<Point> getLayerOutside(Set<Point> wallPoints, Set<Point> layerAroundWall) {
		Set<Point> layerOutside = new HashSet<>();

		if (!layerAroundWall.isEmpty()) {
			//find a top block in layerAroundWall
			Point top = layerAroundWall.iterator().next();
			for (Point p : layerAroundWall) {
				if (p.y() > top.y()) {
					top = p;
				}
			}

			//fill layerOutside
			Point origin = top;
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			Set<Material> traverseMaterials = null; //traverse all block types
			Set<Material> returnMaterials = null; //return all block types
			int rangeLimit = 2 * model.generationRangeLimit + 2;
			Set<Point> ignorePoints = wallPoints;
			Set<Point> searchablePoints = new HashSet<>(layerAroundWall);
			searchablePoints.addAll(getOriginPoints());
			layerOutside = Blocks.getPointsConnected(model.world, origin, originLayer,
					traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();
			layerOutside.addAll(originLayer);
			layerOutside.retainAll(layerAroundWall); //this is needed because we add origin points to searchablePoints
		}

//		Debug.msg("layerOutside.size(): " + layerOutside.size());
		return layerOutside;
	}
	protected Set<Point> getOriginPoints() {
		Set<Point> originPoints = new HashSet<>();
		originPoints.add(model.anchorPoint);
		return originPoints;
	}

	private Set<Point> getPointsInside(Set<Point> layerOutside, Set<Point> layerAroundWall, Set<Point> wallPoints) {
		Set<Point> pointsInside = new HashSet<>();

		if (!layerAroundWall.isEmpty()) {
			//get layerInside
			Set<Point> layerInside = new HashSet<>(layerAroundWall);
			layerInside.removeAll(layerOutside);

			//fill pointsInside
			if (!layerInside.isEmpty()) {
				Point origin = layerInside.iterator().next();
				Set<Point> originLayer = layerInside;
				Set<Material> traverseMaterials = null; //traverse all block types
				Set<Material> returnMaterials = null; //all block types
				int maxReturns = (new Cuboid(model.world, layerInside)).countBlocks() + 1; //set maxReturns as anti near infinite search (just in case)
				int rangeLimit = 2 * model.generationRangeLimit;
				Set<Point> ignorePoints = wallPoints;
				Set<Point> searchablePoints = null; //search all points
				pointsInside = Blocks.getPointsConnected(model.world, origin, originLayer,
						traverseMaterials, returnMaterials, maxReturns, rangeLimit, ignorePoints, searchablePoints).join();
				if (pointsInside.size() == maxReturns) {
					Debug.error("BaseCore::getPointsInside() tried to do infinite search.");
				}
				pointsInside.addAll(originLayer);
			}
		}

//		Debug.msg("pointsInside.size(): " + pointsInside.size());
		return pointsInside;
	}

	private void updateClaimedPoints(Set<Point> wallPoints, Set<Point> layerAroundWall) {
		model.claimedPoints.clear();
		model.claimedWallPoints.clear();

		//claim wallPoints
		model.claimedPoints.addAll(wallPoints);
		model.claimedWallPoints.addAll(wallPoints);

		//claim layerAroundWall
		model.claimedPoints.addAll(layerAroundWall);

		//claim originPoints and layer around
		Set<Point> originPoints = getOriginPoints();
		Set<Point> layerAroundOrigins = getLayerAround(originPoints, Blocks.ConnectedThreshold.POINTS).join(); //should be nearly instant so ok to wait
		model.claimedPoints.addAll(originPoints);
		model.claimedPoints.addAll(layerAroundOrigins);
	}

	private List<Set<Point>> getGeneratableWallLayers() {
		CoreMaterials coreMats = model.animator.getCoreMats();
		coreMats.refresh(); //refresh protectable blocks list based on chest contents

		Set<Point> nearbyClaimedPoints = getClaimedPointsOfNearbyCores();

		//return all connected wall points ignoring (and not traversing) claimedPoints (generationRangeLimit search range)
		Point origin = model.anchorPoint;
		Set<Point> originLayer = new HashSet<>(getOriginPoints());
		originLayer.addAll(getGeneratedPoints());
		Set<Material> traverseMaterials = coreMats.getGeneratableWallMaterials();
		Set<Material> returnMaterials = coreMats.getGeneratableWallMaterials();
		int maxReturns = Math.max(0, FortressPlugin.config_generationBlockLimit - getGeneratedPoints().size());
		int rangeLimit = model.generationRangeLimit;
		Set<Point> ignorePoints = nearbyClaimedPoints;
		Map<Point, Material> pretendPoints = BedrockManager.getMaterialByPointMapForWorld(model.world);
		List<Set<Point>> foundLayers = Blocks.getPointsConnectedAsLayers(model.world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, ignorePoints, pretendPoints).join();

		//correct layer indexes (first non already generated layer is not always layer 0)
		List<Set<Point>> layers = new ArrayList<>();
		int dummyLayerCount = model.animator.getGeneratedLayers().stream()
				.map((layer) -> (layer.isEmpty()) ? 0 : 1)
				.reduce((a, b) -> a + b).orElse(0);
		for (int i = 0; i < dummyLayerCount; i++) {
			layers.add(new HashSet<>());
		}
		layers.addAll(foundLayers);

		return layers;
	}

	private Set<Point> getClaimedPointsOfNearbyCores() {
		Set<BaseCore> nearbyCores = FortressesManager.getOtherCoresInRange(model.anchorPoint, model.generationRangeLimit * 2 + 1); //not sure if the + 1 is needed

		Set<Point> nearbyClaimedPoints = new HashSet<>();
		for (BaseCore core : nearbyCores) {
			nearbyClaimedPoints.addAll(core.getClaimedPoints());
		}

		return nearbyClaimedPoints;
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

//	private void unclaimDisconnected() {
//		//fill pointsToUnclaim
//		Set<Point> pointsToUnclaim = new HashSet<>();
//		Set<Material> wallMaterials = model.animator.getWallMats().getWallMaterials();
//		Set<Point> connectedPoints = getPointsConnected(wallMaterials, wallMaterials, model.generationRangeLimit, null, model.claimedWallPoints);
//		for (Point claimedWallPoint : model.claimedWallPoints) {
//			if (!connectedPoints.contains(claimedWallPoint)) { //found claimed wall point that is now disconnected
//				pointsToUnclaim.add(claimedWallPoint);
//			}
//		}
//
//		//unclaim pointsToUnclaim
//		model.claimedWallPoints.removeAll(pointsToUnclaim);
//		updateClaimedPoints(model.claimedWallPoints).join(); //wait for async
//
//		//degenerate overlap between pointsToUnclaim and generatedLayers
//		Set<Point> pointsToDegenerate = new HashSet<>(pointsToUnclaim);
//		pointsToDegenerate.retainAll(Wall.flattenLayers(model.animator.getGeneratedLayers()));
//		model.animator.degenerate(pointsToDegenerate);
//	}

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

	//used by unclaimDisconnected()?
//	private CompletableFuture<Set<Point>> getPointsConnected(Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
//		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
//		Point origin = model.anchorPoint;
//		Set<Point> originLayer = rune.getPattern().getPoints();
//		return Wall.getPointsConnected(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints);
//	}

	protected CompletableFuture<Set<Point>> getLayerAround(Set<Point> originLayer, Blocks.ConnectedThreshold threshold) {
		Point origin = model.anchorPoint;
		Set<Material> traverseMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = model.generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Blocks.getPointsConnected(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, threshold);
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
