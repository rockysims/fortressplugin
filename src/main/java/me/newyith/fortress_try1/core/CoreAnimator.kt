package me.newyith.fortress_try1.core

import com.fasterxml.jackson.annotation.JsonProperty
import me.newyith.fortress_try1.bedrock.BedrockBatch
import me.newyith.fortress_try1.bedrock.BedrockManager
import me.newyith.fortress_try1.bedrock.timed.TimedBedrockManager
import me.newyith.fortress_try1.core.util.WallLayer
import me.newyith.fortress_try1.event.TickTimer
import me.newyith.fortress_try1.protection.ProtectionBatch
import me.newyith.fortress_try1.protection.ProtectionManager
import me.newyith.fortressOrig.core.CoreParticles
import me.newyith.util.Log
import me.newyith.util.Point
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World

class CoreAnimator(
	@JsonProperty("coreId") val coreId: CoreId,
	@JsonProperty("coreMaterials") val coreMaterials: CoreMaterials
) {
	@JsonProperty("allOldProtectionBatches") private val allOldProtectionBatches: MutableSet<MutableList<ProtectionBatch>> = HashSet()
	@JsonProperty("curProtectionBatches") private var curProtectionBatches: MutableList<ProtectionBatch> = ArrayList()
	@JsonProperty("wallLayers") private var wallLayers: List<WallLayer> = ArrayList()
	@JsonProperty("coreParticles") val coreParticles = CoreParticles()
	@JsonProperty("skipAnimation") private var skipAnimation = false
	@JsonProperty("animationInProgress") private var animationInProgress = false
	@JsonProperty("curIndex") private var curIndex = 0
	@Transient private val ticksPerFrame: Int = (150 / TickTimer.msPerTick) // msPerFrame / msPerTick
	@Transient private var animationWaitTicks = 0
	@Transient val world: World = Bukkit.getWorld(coreId.worldName)
	val anchorPoint: Point get() = coreId.anchorPoint

	fun secondStageLoad() {
		coreParticles.onGeneratedChanges()
		//TODO: load some of the managers (such as ProtectionManagerForCoreId) here?
	}

	fun generate(wallLayers: List<WallLayer>) {
		//add curBatches to allOldBatches so this method is robust enough to get called twice without calling degenerate() in between
		allOldProtectionBatches.add(curProtectionBatches)
		curProtectionBatches = ArrayList()

		this.wallLayers = wallLayers
		this.curIndex = 0
		this.animationInProgress = true
	}

	fun degenerate(skipAnimation: Boolean) {
		allOldProtectionBatches.add(curProtectionBatches)
		curProtectionBatches = ArrayList()

		wallLayers = ArrayList()
		curIndex = 0
		animationInProgress = true

		if (skipAnimation) {
			this.skipAnimation = true
			tick()
			this.skipAnimation = false
		}
	}

	//Note: protectedPoints is the new getGeneratedPoints()
	val protectedPoints: Set<Point> get() {
		//TODO: cache this? (and clear at same time as core.onGeneratedChanged())
		return ProtectionManager.forCoreId(coreId).buildProtectedPoints()
	}

	fun getGeneratableWallMaterials(): Set<Material> {
		coreMaterials.refresh() //refresh protectable blocks list based on chest contents
		return coreMaterials.generatableWallMaterials
	}

	fun getInvalidWallMaterials(): Set<Material> {
		return coreMaterials.invalidWallMaterials
	}

	fun tick() {
		if (animationInProgress) {
			animationWaitTicks++
			if (animationWaitTicks >= ticksPerFrame || skipAnimation) {
				animationWaitTicks = 0

				while (true) {
					//try to update to next frame
					val updatedFrame = updateToNextFrame()
					if (!updatedFrame) {
						//no next frame so stop trying to animate
						animationInProgress = false
						break
					}
					if (updatedFrame && !skipAnimation) {
						//updated to next frame so we're done for now
						break
					}
				}
			}
		}
	}

	// --------- Internal Methods ---------

	private fun updateToNextFrame(): Boolean {
		//try to generate a new layer
		var generatedNewLayer = false
		while (!generatedNewLayer && curIndex < wallLayers.size) {
			//generate this layer
			val layer = wallLayers[curIndex]
			if (layer != null) {
				val protectionBatch = ProtectionBatch(coreId, layer.points)
				val newlyProtecteds = ProtectionManager.forCoreId(coreId).protect(protectionBatch)
				curProtectionBatches.add(protectionBatch)
				alter(protectionBatch, layer.alterPoints)

				if (newlyProtecteds.size > 0) {
					generatedNewLayer = true

					//show bedrock wave (if animation on)
					if (!skipAnimation) {
						val ms = 4 * ticksPerFrame * TickTimer.msPerTick
						TimedBedrockManager.forCoreId(coreId).convert(coreId, newlyProtecteds, ms)
					}
				}
			} else {
				Log.warn("updateToNextFrame() failed to find wallLayer with layerIndex " + curIndex + " (anchor: " + anchorPoint + ")")
			}

			curIndex++
		}

		//try to degenerate last layer of each oldProtectionBatches in allOldProtectionBatches
		var degeneratedOldLayer = false
		val it = allOldProtectionBatches.iterator()
		while (it.hasNext()) {
			val oldProtectionBatches = it.next()
			if (oldProtectionBatches.isEmpty()) {
				it.remove()
				continue
			}
			//we know oldProtectionBatches is not empty now

			//remove and degenerate last batch in oldProtectionBatches
			val lastOldProtectionBatch = oldProtectionBatches.removeAt(oldProtectionBatches.size - 1)
			val newlyUnprotecteds = ProtectionManager.forCoreId(coreId).unprotect(lastOldProtectionBatch)
			unalter(lastOldProtectionBatch)

			if (newlyUnprotecteds.size > 0) {
				degeneratedOldLayer = true

				//show bedrock wave (if animation on)
				if (!skipAnimation) {
					val ms = 4 * ticksPerFrame * TickTimer.msPerTick
					TimedBedrockManager.forCoreId(coreId).convert(coreId, newlyUnprotecteds, ms)
				}
			}
		}

		val updatedToNextFrame = generatedNewLayer || degeneratedOldLayer

		if (updatedToNextFrame) coreParticles.onGeneratedChanges()

		return updatedToNextFrame
	}

	private fun alter(batch: ProtectionBatch, alterPoints: Set<Point>) {
		//convert cobblestone in batch to bedrock
		val bedrockBatch = BedrockBatch(coreId, alterPoints)
		BedrockManager.forCoreId(coreId).convert(bedrockBatch)
		batch.addBedrockBatch(bedrockBatch)
	}

	private fun unalter(protectionBatch: ProtectionBatch) {
		//revert bedrock in protectionBatch to cobblestone
		val bedrockBatches = protectionBatch.removeBedrockBatches()
		for (bedrockBatch in bedrockBatches) {
			BedrockManager.forCoreId(coreId).revert(bedrockBatch)
		}
	}
}