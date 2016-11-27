package me.newyith.fortress.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockManagerNew;
import me.newyith.fortress.bedrock.timed.TimedBedrockManagerNew;
import me.newyith.fortress.core.util.GenPrepData;
import me.newyith.fortress.core.util.WallLayer;
import me.newyith.fortress.core.util.WallLayers;
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
import java.util.stream.Collectors;

public abstract class BaseCore {
	public static class Model {
		protected final Point anchorPoint; //GeneratorCore::showRipple() needs model.anchorPoint to be final (I think)
		protected final Set<Point> claimedPoints;
		protected final Set<Point> claimedWallPoints;
		protected final BedrockAuthToken bedrockAuthToken;
		protected final CoreAnimator animator;
		protected UUID placedByPlayerId;
		protected final Set<Point> layerOutsideFortress;
		protected final Set<Point> pointsInsideFortress;
		protected final String worldName;
		protected final transient World world; //GeneratorCore::showRipple() needs model.world to be final (I think)
		protected final transient int generationRangeLimit;
		protected transient CoreParticles coreParticles;
		protected transient CompletableFuture<GenPrepData> genPrepDataFuture;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("claimedPoints") Set<Point> claimedPoints,
					 @JsonProperty("claimedWallPoints") Set<Point> claimedWallPoints,
					 @JsonProperty("bedrockAuthToken") BedrockAuthToken bedrockAuthToken,
					 @JsonProperty("animator") CoreAnimator animator,
					 @JsonProperty("placedByPlayerId") UUID placedByPlayerId,
					 @JsonProperty("layerOutsideFortress") Set<Point> layerOutsideFortress,
					 @JsonProperty("pointsInsideFortress") Set<Point> pointsInsideFortress,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.claimedPoints = claimedPoints;
			this.claimedWallPoints = claimedWallPoints;
			this.bedrockAuthToken = bedrockAuthToken;
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
			this(m.anchorPoint, m.claimedPoints, m.claimedWallPoints, m.bedrockAuthToken, m.animator, m.placedByPlayerId, m.layerOutsideFortress, m.pointsInsideFortress, m.worldName);
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
		BedrockAuthToken bedrockAuthToken = new BedrockAuthToken();
		CoreAnimator animator = new CoreAnimator(world, anchorPoint, coreMats);
		UUID placedByPlayerId = null; //set by onCreated()
		Set<Point> layerOutsideFortress = new HashSet<>();
		Set<Point> pointsInsideFortress = new HashSet<>();
		String worldName = world.getName();
		model = new Model(anchorPoint, claimedPoints, claimedWallPoints, bedrockAuthToken, animator,
				placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public void shield(Point shieldPoint) {
		Set<Point> shieldPoints = new HashSet<>();
		shieldPoints.add(shieldPoint);
		shield(shieldPoints);
	}

	public void shield(Set<Point> shieldPoints) {
		TimedBedrockManagerNew.forWorld(model.world).convert(model.bedrockAuthToken, shieldPoints);
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

		boolean canOpenDoor = false;
		for (String name : names) {
			if (name.equalsIgnoreCase(playerName)) {
				canOpenDoor = true;
				break;
			}
		}

		return canOpenDoor;
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
		Set<Point> alreadyClaimedPoints = buildClaimedPointsOfNearbyCores();
		boolean overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints); //disjoint means no points in common

		boolean canPlace = !overlapWithClaimed;
		if (canPlace) {
			makeGenPrepDataFuture().thenAccept((data) -> {
				//update claimed points
				ImmutableList<WallLayer> wallLayers = data.wallLayers;
				Set<Point> wallPoints = WallLayers.getAllPointsIn(wallLayers);
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
		BedrockManagerNew.forWorld(model.world).revert(model.bedrockAuthToken);
		FortressesManager.forWorld(model.world).removeClaimedWallPoints(model.claimedWallPoints);
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
				//data.wallLayers should already be merged with any old wallLayers that are still generated
				ImmutableList<WallLayer> wallLayers = data.wallLayers;
				Set<Point> wallPoints = WallLayers.getAllPointsIn(wallLayers);

				//update claimed points
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
		model.genPrepDataFuture = makeGenPrepDataFuture();
	}




	private CompletableFuture<GenPrepData> makeGenPrepDataFuture() {
		//prepare to make future
		CoreMaterials coreMats = model.animator.getCoreMats();
		coreMats.refresh(); //refresh protectable blocks list based on chest contents
		ImmutableSet<Material> wallMaterials = ImmutableSet.copyOf(coreMats.getGeneratableWallMaterials());
		ImmutableSet<Point> originPoints = ImmutableSet.copyOf(getOriginPoints());
		ImmutableSet<Point> nearbyClaimedPoints = ImmutableSet.copyOf(buildClaimedPointsOfNearbyCores());
		ImmutableList<ImmutableSet<Point>> existingLayers = ImmutableList.copyOf(
			model.animator.getWallLayers().parallelStream()
				.map(wallLayer -> ImmutableSet.copyOf(wallLayer.getGeneratedPoints()))
				.collect(Collectors.toList())
		);
		ImmutableMap<Point, Material> pretendPoints = ImmutableMap.copyOf(BedrockManagerNew.forWorld(model.world).getMaterialByPointMap());

		//make future
		CompletableFuture<GenPrepData> future = GenPrepData.makeFuture(
				model.world, model.anchorPoint, originPoints, wallMaterials,
				nearbyClaimedPoints, existingLayers, pretendPoints);

		//fire onSearchingChanged events
		onSearchingChanged(true);
		future.thenAccept((data) -> onSearchingChanged(false)); //thenAccept is not called if future was cancelled

		return future;
	}




	protected abstract void onSearchingChanged(boolean searching);

	public World getWorld() {
		return model.world;
	}

	public Player getOwner() {
		return Bukkit.getPlayer(model.placedByPlayerId);
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
	protected abstract Set<Point> getOriginPoints();

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

	protected void updateClaimedPoints(Set<Point> wallPoints, Set<Point> layerAroundWall) {
		FortressesManager.forWorld(model.world).removeClaimedWallPoints(model.claimedWallPoints);

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

		FortressesManager.forWorld(model.world).addClaimedWallPoints(model.claimedWallPoints, model.anchorPoint);
	}

	//TODO: delete commented out getGeneratableWallLayers() method
//	private List<Set<Point>> getGeneratableWallLayers() {
//		CoreMaterials coreMats = model.animator.getCoreMats();
//		coreMats.refresh(); //refresh protectable blocks list based on chest contents
//
//		Set<Point> nearbyClaimedPoints = buildClaimedPointsOfNearbyCores();
//
//		//return all connected wall points ignoring (and not traversing) nearbyClaimedPoints (generationRangeLimit search range)
//		Point origin = model.anchorPoint;
//		Set<Point> originLayer = new HashSet<>(getOriginPoints());
//		originLayer.addAll(getGeneratedPoints());
//		Set<Material> traverseMaterials = coreMats.getGeneratableWallMaterials();
//		Set<Material> returnMaterials = coreMats.getGeneratableWallMaterials();
//		int maxReturns = Math.max(0, FortressPlugin.config_generationBlockLimit - getGeneratedPoints().size());
//		int rangeLimit = model.generationRangeLimit;
//		Set<Point> ignorePoints = nearbyClaimedPoints;
//		Map<Point, Material> pretendPoints = BedrockManagerNew.forWorld(model.world).getMaterialByPointMap();
//		List<Set<Point>> foundLayers = Blocks.getPointsConnectedAsLayers(model.world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, ignorePoints, pretendPoints).join();
//
//		//correct layer indexes (first non already generated layer is not always layer 0)
//		List<Set<Point>> layers = new ArrayList<>();
//		int dummyLayerCount = model.animator.getGeneratedLayers().stream()
//				.map((layer) -> (layer.isEmpty()) ? 0 : 1)
//				.reduce((a, b) -> a + b).orElse(0);
//		for (int i = 0; i < dummyLayerCount; i++) {
//			layers.add(new HashSet<>());
//		}
//		layers.addAll(foundLayers);
//
//		return layers;
//	}

	private Set<Point> buildClaimedPointsOfNearbyCores() {
		int radius = model.generationRangeLimit * 2 + 1; //not sure if the + 1 is needed
		Set<BaseCore> nearbyCores = FortressesManager.forWorld(model.world).getOtherCoresInRadius(model.anchorPoint, radius);

		Set<Point> nearbyClaimedPoints = new HashSet<>();
		for (BaseCore core : nearbyCores) {
			nearbyClaimedPoints.addAll(core.getClaimedPoints());
		}

		return nearbyClaimedPoints;
	}

	public Set<Point> getClaimedPoints() {
		return model.claimedPoints;
	}

	public Set<Point> getClaimedWallPoints() {
		return model.claimedWallPoints;
	}

	public Set<Point> getGeneratedPoints() {
		return model.animator.getGeneratedPoints();
	}

	public Set<Point> getLayerOutsideFortress() {
		return model.layerOutsideFortress;
	}

	public Set<Point> getPointsInsideFortress() {
		return model.pointsInsideFortress;
	}

	protected CompletableFuture<Set<Point>> getLayerAround(Set<Point> originLayer, Blocks.ConnectedThreshold threshold) {
		Point origin = model.anchorPoint;
		Set<Material> traverseMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = model.generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Blocks.getPointsConnected(model.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, threshold);
	}
}
