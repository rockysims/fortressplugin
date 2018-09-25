package me.newyith.fortress.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.core.util.GenPrepData;
import me.newyith.fortress.core.util.WallLayer;
import me.newyith.fortress.main.BedrockSafety;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.protection.ProtectionAuthToken;
import me.newyith.fortress.protection.ProtectionManager;
import me.newyith.fortress.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class BaseCore {
	public static class Model {
		protected final Point anchorPoint; //GeneratorCore::showRipple() needs model.anchorPoint to be final (I think)
		protected ImmutableSet<Point> claimedPoints;
		protected ImmutableSet<Point> claimedWallPoints;
		protected final BedrockAuthToken bedrockAuthToken;
		protected final ProtectionAuthToken protectionAuthToken;
		protected final CoreAnimator animator;
		protected boolean active;
		protected UUID placedByPlayerId;
		protected Set<Point> layerOutsideFortress;
		protected Set<Point> pointsInsideFortress;
		protected final String worldName;
		protected final transient World world; //GeneratorCore::showRipple() needs model.world to be final (I think)
		protected final transient int generationRangeLimit;
		protected transient CoreParticles coreParticles;
		protected transient CompletableFuture<GenPrepData> genPrepDataFuture;
		protected final transient List<Runnable> actionsToRunOnNextTick;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("claimedPoints") ImmutableSet<Point> claimedPoints,
					 @JsonProperty("claimedWallPoints") ImmutableSet<Point> claimedWallPoints,
					 @JsonProperty("bedrockAuthToken") BedrockAuthToken bedrockAuthToken,
					 @JsonProperty("protectionAuthToken") ProtectionAuthToken protectionAuthToken,
					 @JsonProperty("animator") CoreAnimator animator,
					 @JsonProperty("active") boolean active,
					 @JsonProperty("placedByPlayerId") UUID placedByPlayerId,
					 @JsonProperty("layerOutsideFortress") Set<Point> layerOutsideFortress,
					 @JsonProperty("pointsInsideFortress") Set<Point> pointsInsideFortress,
					 @JsonProperty("worldName") String worldName) {
			this.anchorPoint = anchorPoint;
			this.claimedPoints = claimedPoints;
			this.claimedWallPoints = claimedWallPoints;
			this.bedrockAuthToken = bedrockAuthToken;
			this.protectionAuthToken = protectionAuthToken;
			this.animator = animator;
			this.active = active;
			this.placedByPlayerId = placedByPlayerId;
			this.layerOutsideFortress = layerOutsideFortress;
			this.pointsInsideFortress = pointsInsideFortress;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.generationRangeLimit = FortressPlugin.config_generationRangeLimit;
			this.coreParticles = new CoreParticles();
			this.genPrepDataFuture = null;
			this.actionsToRunOnNextTick = new ArrayList<>();
		}
	}
	protected Model model = null;

	@JsonCreator
	public BaseCore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BaseCore(World world, Point anchorPoint, CoreMaterials coreMats) {
		ImmutableSet<Point> claimedPoints = ImmutableSet.copyOf(new HashSet<>());
		ImmutableSet<Point> claimedWallPoints = ImmutableSet.copyOf(new HashSet<>());
		BedrockAuthToken bedrockAuthToken = new BedrockAuthToken();
		ProtectionAuthToken protectionAuthToken = new ProtectionAuthToken();
		CoreAnimator animator = new CoreAnimator(world, anchorPoint, coreMats, bedrockAuthToken, protectionAuthToken);
		boolean active = false;
		UUID placedByPlayerId = null; //set by onCreated()
		Set<Point> layerOutsideFortress = new HashSet<>();
		Set<Point> pointsInsideFortress = new HashSet<>();
		String worldName = world.getName();
		model = new Model(anchorPoint, claimedPoints, claimedWallPoints, bedrockAuthToken, protectionAuthToken, animator,
				active, placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public void shield(Point shieldPoint) {
		Set<Point> shieldPoints = new HashSet<>();
		shieldPoints.add(shieldPoint);
		shield(shieldPoints);
	}

	public void shield(Set<Point> shieldPoints) {
		TimedBedrockManager.forWorld(model.world).convert(model.bedrockAuthToken, shieldPoints);
	}

	public Cuboid buildCuboid() {
		return new Cuboid(model.world, model.layerOutsideFortress);
	}

	public boolean playerCanOpenDoor(Player player, Point doorPoint) {
		Set<Point> actualSigns = getDoorWhitelistSignPoints(doorPoint);
		if (actualSigns.isEmpty()) {
			actualSigns = getFallbackWhitelistSignPoints();
		}

		return isWhitelistedBy(player, actualSigns);
	}

	public boolean playerCanUseNetherPortal(Player player, Point portalPoint) {
		Set<Point> actualSigns = getNetherPortalWhitelistSignPoints(portalPoint);
		if (actualSigns.isEmpty()) {
			actualSigns = getFallbackWhitelistSignPoints();
		}

		return isWhitelistedBy(player, actualSigns);
	}

	private boolean isWhitelistedBy(Player player, Set<Point> signs) {
		String playerName = player.getName();
		Set<String> names = new HashSet<>();
		for (Point p : signs) {
			if (Blocks.isSign(p.getType(model.world))) {
				Block signBlock = p.getBlock(model.world);
				Sign sign = (Sign)signBlock.getState();
				names.addAll(getNamesFromSign(sign));
			}
		}

		boolean isWhitelisted = false;
		for (String name : names) {
			if (name.equalsIgnoreCase(playerName)) {
				isWhitelisted = true;
				break;
			}
		}

		return isWhitelisted;
	}

	protected Set<Point> getFallbackWhitelistSignPoints() {
		//this method exists only so GeneratorCore can override it
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

	private Set<Point> getNetherPortalWhitelistSignPoints(Point portalPoint) {
		Set<Point> actualSignPoints = new HashSet<>();

		World world = model.world;
		if (portalPoint.is(Material.PORTAL, world)) {
			final boolean portalAxisX = portalPoint.getBlock(world).getData() == 1;
			int rangeLimit = 16;

			//loadAndPreventUnload of chunks within rangeLimit (Blocks.getPointsConnected() treats points in unloaded chunks as having a material type of null)
			ChunkBatch chunksInRange = Chunks.inRange(world, portalPoint, rangeLimit);
			Chunks.loadAndPreventUnload(world, chunksInRange);

			//find portalPoints
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(portalPoint);
			Set<Material> traverseReturnMaterials = new HashSet<>();
			traverseReturnMaterials.add(Material.PORTAL);
			Set<Point> portalPoints = Blocks.getPointsConnected(
					world,
					portalPoint,
					originLayer,
					traverseReturnMaterials,
					traverseReturnMaterials,
					rangeLimit,
					null,
					Blocks.ConnectedThreshold.FACES
			).join().stream()
					.filter(p -> (portalAxisX)
							? p.zInt() == portalPoint.zInt()
							: p.xInt() == portalPoint.xInt())
					.collect(Collectors.toSet());
			portalPoints.add(portalPoint);

			//find framePoints
			traverseReturnMaterials.clear();
			traverseReturnMaterials.add(Material.OBSIDIAN);
			Set<Point> framePoints = Blocks.getPointsConnected(
					world,
					portalPoint,
					portalPoints,
					traverseReturnMaterials,
					traverseReturnMaterials,
					rangeLimit,
					1,
					null,
					Blocks.ConnectedThreshold.POINTS
			).join().stream()
					.filter(p -> (portalAxisX)
							? p.zInt() == portalPoint.zInt()
							: p.xInt() == portalPoint.xInt())
					.collect(Collectors.toSet());

			//find signPoints
			traverseReturnMaterials = Blocks.getSignMaterials();
			Set<Point> signPoints = Blocks.getPointsConnected(
					world,
					portalPoint,
					framePoints,
					traverseReturnMaterials,
					traverseReturnMaterials,
					rangeLimit,
					null,
					Blocks.ConnectedThreshold.FACES
			).join();

			Chunks.allowUnload(world, chunksInRange);

			actualSignPoints.addAll(signPoints);
		}

		return actualSignPoints;
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
				updateClaimedPoints(data.claimedPoints, data.wallPoints);

				//update inside & outside
				model.pointsInsideFortress.clear();
				model.pointsInsideFortress.addAll(data.pointsInside);
				model.layerOutsideFortress.clear();
				model.layerOutsideFortress.addAll(data.layerOutside);

				//tell player how many wall blocks were found
				sendMessage("Fortress generator found " + data.wallPoints.size() + " wall blocks.");
			});
		} else {
			sendMessage("Fortress generator is too close to another generator's wall.");
		}

		return canPlace;
	}

	public void onBroken() {
		degenerateWall(true); //true means skipAnimation
		ProtectionManager.forWorld(model.world).unprotect(model.protectionAuthToken);
		BedrockManager.forWorld(model.world).revert(model.bedrockAuthToken);
		FortressesManager.forWorld(model.world).removeClaimedWallPoints(model.claimedWallPoints);
	}

	public void setActive(boolean active) {
		if (active) {
			generateWall();
		} else {
			degenerateWall(false); //false means don't skipAnimation
		}

		model.active = active;
	}

	public boolean isActive() {
		return model.active;
	}

	public void onGeneratedChanged() {
		model.coreParticles.onGeneratedChanges();
	}

	public void tick() {
		Debug.start("BaseCore::tick()"); //TODO:: delete this line
		synchronized (model.actionsToRunOnNextTick) {
			model.actionsToRunOnNextTick.forEach(Runnable::run);
			model.actionsToRunOnNextTick.clear();
		}

		CompletableFuture<GenPrepData> future = model.genPrepDataFuture;
		if (future != null) {
			GenPrepData data = future.getNow(null);
			if (data != null) {
				model.genPrepDataFuture = null;

				//* Generate
				//data.wallLayers should already be merged with any old wallLayers that are still generated
				ImmutableList<WallLayer> wallLayers = data.wallLayers;
				ImmutableSet<Point> wallPoints = data.wallPoints;

				//update claimed points
				updateClaimedPoints(data.claimedPoints, data.wallPoints);

				//update inside & outside
				model.pointsInsideFortress = data.pointsInside;
				model.layerOutsideFortress = data.layerOutside;

				//record bedrock safety then queue up generate() to run on next tick
				BedrockSafety.record(model.world, wallPoints).thenRun(() -> {
					Runnable generateAction = () -> model.animator.generate(wallLayers);
					synchronized (model.actionsToRunOnNextTick) {
						model.actionsToRunOnNextTick.add(generateAction);
					}
				});
				//*/
			} else {
				//indicate search for what to generate is in progress
				model.coreParticles.displayAnchorParticle(this);
			}
		}

		Debug.start("BaseCore::tick() e"); //TODO:: delete this line
		model.coreParticles.tick(this);
		Debug.end("BaseCore::tick() e"); //TODO:: delete this line


		Debug.start("BaseCore::tick() f"); //TODO:: delete this line

		boolean waitingForGenPrepData = future != null && !future.isDone();
		if (!waitingForGenPrepData) {
			model.animator.tick();

			if (model.animator.isAnimating()) {
				//indicate de/generation is in progress
				model.coreParticles.tickAnimationParticles(this);
			}
		}
		Debug.end("BaseCore::tick() f"); //TODO:: delete this line
		Debug.end("BaseCore::tick()"); //TODO:: delete this line
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
		ImmutableSet<Material> wallMaterials = ImmutableSet.copyOf(model.animator.getGeneratableWallMaterials());
		ImmutableSet<Point> originPoints = ImmutableSet.copyOf(getOriginPoints());
		ImmutableSet<Point> nearbyClaimedPoints = ImmutableSet.copyOf(buildClaimedPointsOfNearbyCores());
		ImmutableMap<Point, Material> pretendPoints = ImmutableMap.copyOf(BedrockManager.forWorld(model.world).getOrBuildMaterialByPointMap());

		//make future
		CompletableFuture<GenPrepData> future = GenPrepData.makeFuture(
				model.world, model.anchorPoint, originPoints, wallMaterials,
				nearbyClaimedPoints, pretendPoints);

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

	protected abstract Set<Point> getOriginPoints();

	private void updateClaimedPoints(ImmutableSet<Point> claimedPoints, ImmutableSet<Point> claimedWallPoints) {
		FortressesManager.forWorld(model.world).removeClaimedWallPoints(model.claimedWallPoints);

		model.claimedPoints = claimedPoints;
		model.claimedWallPoints = claimedWallPoints;

		FortressesManager.forWorld(model.world).addClaimedWallPoints(model.claimedWallPoints, model.anchorPoint);
	}

	private Set<Point> buildClaimedPointsOfNearbyCores() {
		int radius = model.generationRangeLimit * 2 + 1; //not sure if the + 1 is needed
		Set<BaseCore> nearbyCores = FortressesManager.forWorld(model.world).getOtherCoresInRadius(model.anchorPoint, radius);

		Set<Point> nearbyClaimedPoints = new HashSet<>();
		for (BaseCore core : nearbyCores) {
			nearbyClaimedPoints.addAll(core.getClaimedPoints());
		}

		return nearbyClaimedPoints;
	}

	public ImmutableSet<Point> getClaimedPoints() {
		return model.claimedPoints;
	}

	public ImmutableSet<Point> getClaimedWallPoints() {
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
