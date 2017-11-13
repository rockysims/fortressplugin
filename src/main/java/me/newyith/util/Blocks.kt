package me.newyith.util

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Uninterruptibles
import org.bukkit.Material
import org.bukkit.World
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet

object Blocks {
	enum class ConnectedThreshold {
		POINTS,
		//LINES,
		FACES
	}

	fun flattenLayers(layers: List<Set<Point>>): Set<Point> {
		val points = HashSet<Point>()

		for (layer in layers) {
			points.addAll(layer)
		}

		return points
	}

	fun getPointsConnected(world: World, origin: Point, originLayer: Set<Point>, traverseMaterials: Set<Material>, returnMaterials: Set<Material>, maxReturns: Int, rangeLimit: Int, ignorePoints: Set<Point>, searchablePoints: Set<Point>): CompletableFuture<Set<Point>> {
		return CompletableFuture.supplyAsync {
			val layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, -1, ignorePoints, searchablePoints, null, ConnectedThreshold.FACES).join()
			flattenLayers(layers)
		}
	}

	fun getPointsConnected(world: World, origin: Point, originLayer: Set<Point>, traverseMaterials: Set<Material>, returnMaterials: Set<Material>, rangeLimit: Int, ignorePoints: Set<Point>, searchablePoints: Set<Point>): CompletableFuture<Set<Point>> {
		return CompletableFuture.supplyAsync {
			val layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, -1, rangeLimit, -1, ignorePoints, searchablePoints, null, ConnectedThreshold.FACES).join()
			flattenLayers(layers)
		}
	}

	fun getPointsConnected(world: World, origin: Point, originLayer: Set<Point>, traverseMaterials: Set<Material>, returnMaterials: Set<Material>, rangeLimit: Int, ignorePoints: Set<Point>, connectedThreshold: ConnectedThreshold): CompletableFuture<Set<Point>> {
		return CompletableFuture.supplyAsync {
			val layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, -1, rangeLimit, -1, ignorePoints, null, null, connectedThreshold).join()
			flattenLayers(layers)
		}
	}

	fun getPointsConnectedAsLayers(world: World, origin: Point, layerLimit: Int, searchablePoints: Set<Point>): CompletableFuture<List<Set<Point>>> {
		val originLayer = HashSet<Point>()
		originLayer.add(origin)
		val rangeLimit = layerLimit + 1
		return getPointsConnectedAsLayers(world, origin, originLayer, null, null, -1, rangeLimit, layerLimit, null, searchablePoints, null, ConnectedThreshold.FACES)
	}

	fun getPointsConnectedAsLayers(world: World, origin: Point, originLayer: MutableSet<Point>?, traverseMaterials: Set<Material>, returnMaterials: Set<Material>, maxReturns: Int, rangeLimit: Int, ignorePoints: Set<Point>, pretendPoints: Map<Point, Material>): CompletableFuture<List<Set<Point>>> {
		var originLayer = originLayer
		if (originLayer == null) {
			originLayer = HashSet<Point>()
			originLayer.add(origin)
		}
		return getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, -1, ignorePoints, null, pretendPoints, ConnectedThreshold.FACES)
	}

	/**
	 * Looks at all blocks connected to the originLayer by traverseMaterials (directly or recursively).
	 *
	 * @param origin The rangeLimit is calculated relative to this point.
	 * @param originLayer The first point(s) to search outward from.
	 * @param traverseMaterials List of connecting block types. If null, all materials traversable.
	 * @param returnMaterials List of block types to look for and return when connected to the wall or null to return all block types.
	 * @param maxReturns Maximum number of points found before returning. If -1, unlimited.
	 * @param rangeLimit The maximum distance away from origin to search.
	 * @param layerLimit The maximum number of layers to search. If -1, unlimited;
	 * @param ignorePoints When searching, these points will be ignored (not traversed or returned). If null, no points ignored.
	 * @param searchablePoints When searching, only these points will be visited (traversed and/or returned). If null, all points searchable.
	 * @param pretendPoints Points to pretend are a different material. If null, no points will be pretend.
	 * @param connectedThreshold Whether connected means 3x3x3 area or only the 6 blocks connected by faces.
	 * @return List of all points (blocks) connected to the originLayer by traverseMaterials and matching a block type in returnMaterials.
	 */
	fun getPointsConnectedAsLayers(world: World, origin: Point, originLayer: Set<Point>, traverseMaterials: Set<Material>?, returnMaterials: Set<Material>?, maxReturns: Int, rangeLimit: Int, layerLimit: Int, ignorePoints: Set<Point>?, searchablePoints: Set<Point>?, pretendPointsOrNull: Map<Point, Material>?, connectedThreshold: ConnectedThreshold): CompletableFuture<List<Set<Point>>> {
		val pretendPoints = pretendPointsOrNull ?: HashMap<Point, Material>()

		return CompletableFuture.supplyAsync<List<Set<Point>>> {
			var matchesAsLayers: MutableList<MutableSet<Point>> = ArrayList()
//			val connected = HashSet<Point>()

			val visited = HashSet<Point>(1000)
			var nextLayer: Deque<Point> = ArrayDeque()
			var layerIndex = -1

			//fill nextLayer and visited from originLayer
			nextLayer.addAll(originLayer)
			visited.addAll(originLayer)

			val recursionLimit2Max = 10 * 6 * Math.pow((rangeLimit * 2).toDouble(), 2.0).toInt()
			var recursionLimit = Math.pow((rangeLimit / 2).toDouble(), 3.0).toInt()
			var lastSleepEnd = System.currentTimeMillis()
			var matchCount = 0
			var sleeplessCount = 0 //just for debugging
			while (!nextLayer.isEmpty()) {
				if (recursionLimit-- <= 0) {
					Log.error("Wall recursionLimit exhausted")
					break
				}

				layerIndex++
				val layer = nextLayer
				nextLayer = ArrayDeque<Point>()

				//process layer
				var recursionLimit2 = recursionLimit2Max
				while (!layer.isEmpty()) {
					//consider sleeping (sleep after 15ms running)
					val elapsed = System.currentTimeMillis() - lastSleepEnd
					if (elapsed > 15) { //use "> 15" except when debugging
						Log.log("Sleeping after not sleeping $sleeplessCount times.")
						Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS) //use "50"ms except when debugging
						lastSleepEnd = System.currentTimeMillis()
						sleeplessCount = 0
					} else {
						sleeplessCount++
					}

					if (recursionLimit2-- <= 0) {
						Log.error("Wall recursionLimit2 exhausted")
						break
					}

					val center = layer.pop()
					val connected = when(connectedThreshold) {
						ConnectedThreshold.FACES -> center.getAdjacent6() as HashSet<Point>
						ConnectedThreshold.POINTS -> center.getAdjacent26() as HashSet<Point>
					}

					//process connected points
					for (p in connected) {
						if (p !in visited) {
							visited.add(p)

							//ignore ignorePoints
							if (ignorePoints != null && p in ignorePoints) continue

							//ignore unsearchable points
							if (searchablePoints != null && p !in searchablePoints) continue

							//ignore out of range points
							if (!isInRange(p, origin, rangeLimit)) continue

							val mat = pretendPoints[p] ?: p.getType(world)

							//add to matchesAsLayers if it matches a returnMaterials type
							if (returnMaterials == null || mat in returnMaterials) {
								//"while" not "if" because maybe only matching blocks are far away but connected by wall
								while (layerIndex >= matchesAsLayers.size) {
									matchesAsLayers.add(HashSet())
								}
								matchesAsLayers[layerIndex].add(p)
								matchCount++
							}

							//consider adding point to nextLayer
							if (traverseMaterials == null || traverseMaterials.contains(mat)) {
								nextLayer.push(p)
							}

							if (maxReturns != -1 && matchCount >= maxReturns) break
						}
					} //end of for loop
					if (maxReturns != -1 && matchCount >= maxReturns) break
				} //end of inner while
				if (maxReturns != -1 && matchCount >= maxReturns) break

				if (layerLimit != -1 && matchesAsLayers.size >= layerLimit) {
					matchesAsLayers = matchesAsLayers.subList(0, layerLimit) //enforce layerLimit (since multiple layers might have been added)
					break
				}
			} //end of outer while

			ImmutableList.copyOf(matchesAsLayers)
		}
	}

	private fun isInRange(p: Point, origin: Point, rangeLimit: Int): Boolean {
		return Math.abs(p.xInt - origin.xInt) <= rangeLimit
			&& Math.abs(p.yInt - origin.yInt) <= rangeLimit
			&& Math.abs(p.zInt - origin.zInt) <= rangeLimit
	}
}