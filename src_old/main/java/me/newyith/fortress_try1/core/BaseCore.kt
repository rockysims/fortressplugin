package me.newyith.fortress_try1.core

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import me.newyith.fortress_try1.protection.ProtectionBatch
import me.newyith.fortress_try1.bedrock.BedrockManager
import me.newyith.fortress_try1.bedrock.timed.TimedBedrockManager
import me.newyith.fortress_try1.core.CoreParticles
import me.newyith.fortress_try1.core.util.GenPrepData
import me.newyith.fortress_try1.core.util.WallLayers
import me.newyith.fortress_try1.main.BedrockSafety
import me.newyith.fortress_try1.main.FortressPlugin
import me.newyith.fortress_try1.main.FortressesManager
import me.newyith.fortress_try1.protection.ProtectionManager
import me.newyith.util.Blocks
import me.newyith.util.Cuboid
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashSet

/**
 * Represents a core (such as a GeneratorCore) and is responsible for orchestrating protection of structure(s).
 * Also responsible for orchestrating all the protection related features such as bedrock ripple, wall particles, etc.
 */
abstract class BaseCore(
	@JsonProperty("animator") protected val animator: CoreAnimator
) {
	@JsonProperty("claimedPoints") val claimedPoints: Set<Point> = HashSet()
	@JsonProperty("claimedWallPoints") val claimedWallPoints: Set<Point> = HashSet()
	@JsonProperty("active") protected var active: Boolean = false
	@JsonProperty("placedByPlayerId") protected var placedByPlayerId: UUID? = null //set by onCreated()
	@JsonProperty("layerOutsideFortress") val layerOutsideFortress: Set<Point> = HashSet()
	@JsonProperty("pointsInsideFortress") val pointsInsideFortress: Set<Point> = HashSet()
	@Transient val world: World = Bukkit.getWorld(worldName)
	val coreId: CoreId get() = animator.coreId
	val worldName: String get() = coreId.worldName
	val anchorPoint: Point get() = coreId.anchorPoint
	val coreMaterials: CoreMaterials get() = animator.coreMaterials
	val coreParticles: CoreParticles get() = animator.coreParticles
	val protectedBatches: List<ProtectionBatch> get() = animator.protectedBatches //new in BaseCore



	constructor(worldName: String, anchorPoint: Point, coreMaterials: CoreMaterials)
		: this(CoreAnimator(CoreId(worldName, anchorPoint), coreMaterials))



	fun secondStageLoad() {
		//TODO: load all the managers here? such as ProtectionManagerForCoreId

		//load managers for coreId
		ProtectionManager.forCoreId(coreId)



		animator.secondStageLoad()
	}





	class Model @JsonCreator
	constructor(@param:JsonProperty("anchorPoint") protected val anchorPoint: Point //GeneratorCore::showRipple() needs model.anchorPoint to be final (I think)
				,
				@JsonProperty("claimedPoints") claimedPoints: Set<Point>,
				@JsonProperty("claimedWallPoints") claimedWallPoints: Set<Point>,
				@param:JsonProperty("bedrockAuthToken") protected val bedrockAuthToken: BedrockAuthToken,
				@param:JsonProperty("protectionAuthToken") protected val protectionAuthToken: ProtectionAuthToken,
				@param:JsonProperty("animator") protected val animator: CoreAnimator,
				@param:JsonProperty("active") protected var active: Boolean,
				@param:JsonProperty("placedByPlayerId") protected var placedByPlayerId: UUID,
				@JsonProperty("layerOutsideFortress") layerOutsideFortress: Set<Point>,
				@JsonProperty("pointsInsideFortress") pointsInsideFortress: Set<Point>,
				@param:JsonProperty("worldName") protected val worldName: String) {
		protected val claimedPoints: MutableSet<Point>
		protected val claimedWallPoints: MutableSet<Point>
		protected val layerOutsideFortress: MutableSet<Point>
		protected val pointsInsideFortress: MutableSet<Point>
		@Transient protected val world: World //GeneratorCore::showRipple() needs model.world to be final (I think)
		@Transient protected val generationRangeLimit: Int
		@Transient protected var coreParticles: CoreParticles
		@Transient protected var genPrepDataFuture: CompletableFuture<GenPrepData>? = null

		init {
			this.claimedPoints = claimedPoints
			this.claimedWallPoints = claimedWallPoints
			this.layerOutsideFortress = layerOutsideFortress
			this.pointsInsideFortress = pointsInsideFortress

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName)
			this.generationRangeLimit = FortressPlugin.config_generationRangeLimit
			this.coreParticles = CoreParticles()
			this.genPrepDataFuture = null
		}

		constructor(m: Model) : this(m.anchorPoint, m.claimedPoints, m.claimedWallPoints, m.bedrockAuthToken, m.protectionAuthToken,
			m.animator, m.active, m.placedByPlayerId, m.layerOutsideFortress, m.pointsInsideFortress, m.worldName) {
		}
	}

	protected var model: Model? = null

	@JsonCreator
	fun BaseCore(@JsonProperty("model") model: Model): ??? {
		this.model = model
	}

	fun BaseCore(world: World, anchorPoint: Point, coreMats: CoreMaterials): ??? {
		val claimedPoints = HashSet<Point>()
		val claimedWallPoints = HashSet<Point>()
		val bedrockAuthToken = BedrockAuthToken()
		val protectionAuthToken = ProtectionAuthToken()
		val animator = CoreAnimator(world, anchorPoint, coreMats, bedrockAuthToken, protectionAuthToken)
		val active = false
		val placedByPlayerId: UUID? = null //set by onCreated()
		val layerOutsideFortress = HashSet<Point>()
		val pointsInsideFortress = HashSet<Point>()
		val worldName = world.name
		model = Model(anchorPoint, claimedPoints, claimedWallPoints, bedrockAuthToken, protectionAuthToken, animator,
			active, placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName)
	}

	//------------------------------------------------------------------------------------------------------------------

	fun shield(shieldPoint: Point) {
		val shieldPoints = HashSet<Point>()
		shieldPoints.add(shieldPoint)
		shield(shieldPoints)
	}

	fun shield(shieldPoints: Set<Point>) {
		TimedBedrockManager.forWorld(model!!.world).convert(model!!.bedrockAuthToken, shieldPoints)
	}

	fun playerCanOpenDoor(player: Player, doorPoint: Point): Boolean {
		val playerName = player.name
		var actualSigns = getDoorWhitelistSignPoints(doorPoint)
		if (actualSigns.isEmpty()) {
			actualSigns = getFallbackWhitelistSignPoints()
		}

		val names = HashSet<String>()
		for (p in actualSigns) {
			if (Blocks.isSign(p.getBlock(model!!.world).type)) {
				val signBlock = p.getBlock(model!!.world)
				val sign = signBlock.state as Sign
				names.addAll(getNamesFromSign(sign))
			}
		}

		var canOpenDoor = false
		for (name in names) {
			if (name.equals(playerName, ignoreCase = true)) {
				canOpenDoor = true
				break
			}
		}

		return canOpenDoor
	}

	protected fun getFallbackWhitelistSignPoints(): Set<Point> {
		//this method exists so GeneratorCore can override it
		return HashSet()
	}

	private fun getDoorWhitelistSignPoints(doorPoint: Point): Set<Point> {
		val potentialSigns = HashSet<Point>()
		val isTrapDoor = Blocks.isTrapDoor(doorPoint.getBlock(model!!.world).type)

		if (isTrapDoor) {
			potentialSigns.addAll(Blocks.getAdjacent6(doorPoint))
		} else {
			//potentialSigns.addAll(point above door and points adjacent to it)
			val aboveDoorPoint = Point(doorPoint).add(0.0, 1.0, 0.0)
			potentialSigns.addAll(Blocks.getAdjacent6(aboveDoorPoint))
			potentialSigns.add(aboveDoorPoint)
		}

		if (signMustBeInside(doorPoint)) {
			potentialSigns.retainAll(model!!.pointsInsideFortress)
		}

		//fill actualSigns (from potentialSigns)
		val actualSigns = HashSet<Point>()
		for (potentialSign in potentialSigns) {
			val mat = potentialSign.getBlock(model!!.world).type
			if (Blocks.isSign(mat)) {
				actualSigns.add(potentialSign)
			}
		}

		//potentialSigns.addAll(connected signs)
		val traverseMaterials = Blocks.getSignMaterials()
		val returnMaterials = Blocks.getSignMaterials()
		val rangeLimit = model!!.generationRangeLimit * 2
		val ignorePoints: Set<Point>? = null
		val searchablePoints: Set<Point>? = null
		val connectedSigns = Blocks.getPointsConnected(model!!.world, doorPoint, actualSigns,
			traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join()
		actualSigns.addAll(connectedSigns)

		return actualSigns
	}

	private fun signMustBeInside(doorPoint: Point): Boolean {
		var signMustBeInside = false

		//fill adjacentToDoor
		val adjacentToDoor = HashSet<Point>()
		val doorPoints = HashSet<Point>()
		doorPoints.add(doorPoint)
		val doorIsTrapDoor = Blocks.isTrapDoor(doorPoint.getBlock(model!!.world).type)
		if (!doorIsTrapDoor) {
			doorPoints.add(Point(doorPoint).add(0.0, -1.0, 0.0))
		}
		for (p in doorPoints) {
			adjacentToDoor.addAll(Blocks.getAdjacent6(p))
		}
		adjacentToDoor.removeAll(doorPoints)

		//if any of the blocks adjacent to door point(s) are pointsInsideFortress, signMustBeInside = true
		val allAdjacentAreOutside = Collections.disjoint(adjacentToDoor, model!!.pointsInsideFortress)
		signMustBeInside = !allAdjacentAreOutside

		return signMustBeInside
	}

	private fun getNamesFromSign(sign: Sign?): Set<String> {
		val names = HashSet<String>()

		if (sign != null) {
			val sb = StringBuilder()
			sb.append(sign.getLine(0))
			sb.append("\n")
			sb.append(sign.getLine(1))
			sb.append("\n")
			sb.append(sign.getLine(2))
			sb.append("\n")
			sb.append(sign.getLine(3))
			var s = sb.toString().replace(" ".toRegex(), "")

			if (s.contains(",")) {
				s = s.replace("\n".toRegex(), "")
				for (name in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
					names.add(name)
				}
			} else {
				for (name in s.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
					names.add(name)
				}
			}
		}

		return names
	}

	// - Events -

	fun onCreated(placingPlayer: Player): Boolean {
		model!!.placedByPlayerId = placingPlayer.uniqueId

		//set overlapWithClaimed = true if placed generator is connected (by faces) to another generator's claimed points
		val claimPoints = getOriginPoints()
		val alreadyClaimedPoints = buildClaimedPointsOfNearbyCores()
		val overlapWithClaimed = !Collections.disjoint(alreadyClaimedPoints, claimPoints) //disjoint means no points in common

		val canPlace = !overlapWithClaimed
		if (canPlace) {
			makeGenPrepDataFuture().thenAccept { data ->
				//update claimed points
				val wallLayers = data.wallLayers
				val wallPoints = WallLayers.getAllPointsIn(wallLayers)
				updateClaimedPoints(wallPoints, data.layerAroundWall)

				//update inside & outside
				model!!.pointsInsideFortress.clear()
				model!!.pointsInsideFortress.addAll(data.pointsInside)
				model!!.layerOutsideFortress.clear()
				model!!.layerOutsideFortress.addAll(data.layerOutside)

				//tell player how many wall blocks were found
				sendMessage("Fortress generator found " + wallPoints.size + " wall blocks.")
			}
		} else {
			sendMessage("Fortress generator is too close to another generator's wall.")
		}

		return canPlace
	}

	fun onBroken() {
		degenerateWall(true) //true means skipAnimation
		ProtectionManager.forWorld(model!!.world).unprotect(model!!.protectionAuthToken)
		BedrockManager.forWorld(model!!.world).revert(model!!.bedrockAuthToken)
		FortressesManager.forWorld(model!!.world).removeClaimedWallPoints(model!!.claimedWallPoints)
	}

//	fun setActive(active: Boolean) {
//		if (active) {
//			generateWall()
//		} else {
//			degenerateWall(false) //false means don't skipAnimation
//		}
//
//		model!!.active = active
//	}
//
//	fun isActive(): Boolean {
//		return model!!.active
//	}

	//TODO: delete this function
//	fun onGeneratedChanged() {
//		model!!.coreParticles.onGeneratedChanges()
//	}

	fun tick() {
		val future = model!!.genPrepDataFuture
		if (future != null) {
			val data = future.getNow(null)
			if (data != null) {
				model!!.genPrepDataFuture = null

				//* Generate
				//data.wallLayers should already be merged with any old wallLayers that are still generated
				val wallLayers = data.wallLayers
				val wallPoints = WallLayers.getAllPointsIn(wallLayers)

				//update claimed points
				updateClaimedPoints(wallPoints, data.layerAroundWall)

				//update inside & outside
				model!!.pointsInsideFortress.clear()
				model!!.pointsInsideFortress.addAll(data.pointsInside)
				model!!.layerOutsideFortress.clear()
				model!!.layerOutsideFortress.addAll(data.layerOutside)

				//bedrock safety
				BedrockSafety.record(model!!.world, wallPoints)

				model!!.animator.generate(wallLayers)

				//*/
			} else {
				model!!.coreParticles.displayAnchorParticle(this)
			}
		}

		model!!.coreParticles.tick(this)

		val waitingForGenPrepData = future != null && !future.isDone
		if (!waitingForGenPrepData) {
			model!!.animator.tick()
		}
	}

	// --------- Internal Methods ---------

	private fun sendMessage(msg: String) {
		var msg = msg
		msg = ChatColor.AQUA.toString() + msg
		Bukkit.getPlayer(model!!.placedByPlayerId).sendMessage(msg)
	}

	/**
	 * Degenerates (turns off) the wall being generated by this generator.
	 */
	private fun degenerateWall(skipAnimation: Boolean) {
		//		Debug.msg("degenerateWall(" + String.valueOf(skipAnimation) + ")");

		//cancel pending generation (if any)
		if (model!!.genPrepDataFuture != null) {
			model!!.genPrepDataFuture!!.cancel(true) //apparently cancelling CompletableFuture doesn't actually stop process. it just doesn't resolve
			model!!.genPrepDataFuture = null
		}

		//getClaimedPointsOfNearbyGenerators(); //make nearby generators look for and degenerate any claimed but unconnected blocks
		model!!.animator.degenerate(skipAnimation)
	}

	/**
	 * Generates (turns on) the wall touching this generator.
	 * Assumes checking for permission to generate walls is already done.
	 */
	private fun generateWall() {
		//		Debug.msg("generateWall()");

		//cancel pending generation (if any)
		if (model!!.genPrepDataFuture != null) {
			model!!.genPrepDataFuture!!.cancel(true) //apparently cancelling CompletableFuture doesn't actually stop process. it just doesn't resolve
			model!!.genPrepDataFuture = null
		}

		//start preparing for generation (tick() handles it when future completes)
		model!!.genPrepDataFuture = makeGenPrepDataFuture()
	}


	private fun makeGenPrepDataFuture(): CompletableFuture<GenPrepData> {
		//prepare to make future
		val wallMaterials = ImmutableSet.copyOf(model!!.animator.generatableWallMaterials)
		val originPoints = ImmutableSet.copyOf(getOriginPoints())
		val nearbyClaimedPoints = ImmutableSet.copyOf(buildClaimedPointsOfNearbyCores())
		val pretendPoints = ImmutableMap.copyOf(BedrockManager.forWorld(model!!.world).materialByPointMap)

		//make future
		val future = GenPrepData.makeFuture(
			model!!.world, model!!.anchorPoint, originPoints, wallMaterials,
			nearbyClaimedPoints, pretendPoints)

		//fire onSearchingChanged events
		onSearchingChanged(true)
		future.thenAccept { data -> onSearchingChanged(false) } //thenAccept is not called if future was cancelled

		return future
	}


	protected abstract fun onSearchingChanged(searching: Boolean)

//	fun getWorld(): World {
//		return model!!.world
//	}

	fun getOwner(): Player {
		return Bukkit.getPlayer(model!!.placedByPlayerId)
	}

	private fun getLayerOutside(wallPoints: Set<Point>, layerAroundWall: Set<Point>): Set<Point> {
		var layerOutside: MutableSet<Point> = HashSet()

		if (!layerAroundWall.isEmpty()) {
			//find a top block in layerAroundWall
			var top = layerAroundWall.iterator().next()
			for (p in layerAroundWall) {
				if (p.y() > top.y()) {
					top = p
				}
			}

			//fill layerOutside
			val origin = top
			val originLayer = HashSet<Point>()
			originLayer.add(origin)
			val traverseMaterials: Set<Material>? = null //traverse all block types
			val returnMaterials: Set<Material>? = null //return all block types
			val rangeLimit = 2 * model!!.generationRangeLimit + 2
			val searchablePoints = HashSet(layerAroundWall)
			searchablePoints.addAll(getOriginPoints())
			layerOutside = Blocks.getPointsConnected(model!!.world, origin, originLayer,
				traverseMaterials, returnMaterials, rangeLimit, wallPoints, searchablePoints).join()
			layerOutside.addAll(originLayer)
			layerOutside.retainAll(layerAroundWall) //this is needed because we add origin points to searchablePoints
		}

		//		Debug.msg("layerOutside.size(): " + layerOutside.size());
		return layerOutside
	}

	protected abstract fun getOriginPoints(): Set<Point>

	private fun getPointsInside(layerOutside: Set<Point>, layerAroundWall: Set<Point>, wallPoints: Set<Point>): Set<Point> {
		var pointsInside: MutableSet<Point> = HashSet()

		if (!layerAroundWall.isEmpty()) {
			//get layerInside
			val layerInside = HashSet(layerAroundWall)
			layerInside.removeAll(layerOutside)

			//fill pointsInside
			if (!layerInside.isEmpty()) {
				val origin = layerInside.iterator().next()
				val traverseMaterials: Set<Material>? = null //traverse all block types
				val returnMaterials: Set<Material>? = null //all block types
				val maxReturns = Cuboid(model!!.world, layerInside).countBlocks() + 1 //set maxReturns as anti near infinite search (just in case)
				val rangeLimit = 2 * model!!.generationRangeLimit
				val searchablePoints: Set<Point>? = null //search all points
				pointsInside = Blocks.getPointsConnected(model!!.world, origin, layerInside,
					traverseMaterials, returnMaterials, maxReturns, rangeLimit, wallPoints, searchablePoints).join()
				if (pointsInside.size == maxReturns) {
					Debug.error("BaseCore::getPointsInside() tried to do infinite search.")
				}
				pointsInside.addAll(layerInside)
			}
		}

		//		Debug.msg("pointsInside.size(): " + pointsInside.size());
		return pointsInside
	}

	protected fun updateClaimedPoints(wallPoints: Set<Point>, layerAroundWall: Set<Point>) {
		FortressesManager.forWorld(model!!.world).removeClaimedWallPoints(model!!.claimedWallPoints)

		model!!.claimedPoints.clear()
		model!!.claimedWallPoints.clear()

		//claim wallPoints
		model!!.claimedPoints.addAll(wallPoints)
		model!!.claimedWallPoints.addAll(wallPoints)

		//claim layerAroundWall
		model!!.claimedPoints.addAll(layerAroundWall)

		//claim originPoints and layer around
		val originPoints = getOriginPoints()
		val layerAroundOrigins = getLayerAround(originPoints, Blocks.ConnectedThreshold.POINTS).join() //should be nearly instant so ok to wait
		model!!.claimedPoints.addAll(originPoints)
		model!!.claimedPoints.addAll(layerAroundOrigins)

		FortressesManager.forWorld(model!!.world).addClaimedWallPoints(model!!.claimedWallPoints, model!!.anchorPoint)
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
//		Map<Point, Material> pretendPoints = BedrockManager.forWorld(model.world).getMaterialByPointMap();
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

	private fun buildClaimedPointsOfNearbyCores(): Set<Point> {
		val radius = model!!.generationRangeLimit * 2 + 1 //not sure if the + 1 is needed
		val nearbyCores = FortressesManager.forWorld(model!!.world).getOtherCoresInRadius(model!!.anchorPoint, radius)

		val nearbyClaimedPoints = HashSet<Point>()
		for (core in nearbyCores) {
			nearbyClaimedPoints.addAll(core.claimedPoints)
		}

		return nearbyClaimedPoints
	}

//	fun getClaimedPoints(): Set<Point> {
//		return model!!.claimedPoints
//	}
//
//	fun getClaimedWallPoints(): Set<Point> {
//		return model!!.claimedWallPoints
//	}

	fun getGeneratedPoints(): Set<Point> {
		return model!!.animator.generatedPoints
	}

//	fun getLayerOutsideFortress(): Set<Point> {
//		return model!!.layerOutsideFortress
//	}
//
//	fun getPointsInsideFortress(): Set<Point> {
//		return model!!.pointsInsideFortress
//	}

	protected fun getLayerAround(originLayer: Set<Point>, threshold: Blocks.ConnectedThreshold): CompletableFuture<Set<Point>> {
		val origin = model!!.anchorPoint
		val traverseMaterials = HashSet<Material>() //no blocks are traversed
		val returnMaterials: Set<Material>? = null //all blocks are returned
		val rangeLimit = model!!.generationRangeLimit + 1
		val ignorePoints: Set<Point>? = null //no points ignored
		return Blocks.getPointsConnected(model!!.world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, threshold)
	}




}