package me.newyith.fortress.util;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//fully written again
public class Blocks {
	public enum ConnectedThreshold {
		POINTS,
		//LINES,
		FACES
	};

	public static Set<Point> flattenLayers(List<Set<Point>> layers) {
		Set<Point> points = new HashSet<>();

		for (Set<Point> layer : layers) {
			points.addAll(layer);
		}

		return points;
	}

	public static List<Set<Point>> merge(List<Set<Point>> layers1, List<Set<Point>> layers2) {
		List<Set<Point>> layers = new ArrayList<>();

		int biggestSize = Math.max(layers1.size(), layers2.size());
		for (int i = 0; i < biggestSize; i++) {
			//process layer i
			layers.add(new HashSet<>());
			if (i < layers1.size()) {
				layers.get(i).addAll(layers1.get(i));
			}
			if (i < layers2.size()) {
				layers.get(i).addAll(layers2.get(i));
			}
		}

		return layers;
	}

	public static Set<Point> getAdjacent6(Point p) {
		Set<Point> points = new HashSet<>();

		points.add(p.add(1, 0, 0));
		points.add(p.add(-1, 0, 0));
		points.add(p.add(0, 1, 0));
		points.add(p.add(0, -1, 0));
		points.add(p.add(0, 0, 1));
		points.add(p.add(0, 0, -1));

		return points;
	}

	public static boolean isAiry(Point dest, World world) {
		boolean airy = true;
		Material mat = dest.getBlock(world).getType();
		airy = airy && !mat.isSolid();
		airy = airy && mat != Material.FIRE;
		airy = airy && mat != Material.WATER;
		airy = airy && mat != Material.LAVA;
		airy = airy && mat != Material.LEGACY_STATIONARY_WATER;
		airy = airy && mat != Material.LEGACY_STATIONARY_LAVA;
		return airy;
	}

	public static boolean isDoor(Material mat) {
		return isTrapDoor(mat) || isTallDoor(mat);
	}

	public static boolean isTallDoor(Material mat) {
		boolean isTallDoor = false;
		switch (mat) {
			case LEGACY_IRON_DOOR_BLOCK:
			case LEGACY_WOODEN_DOOR:
			case ACACIA_DOOR:
			case BIRCH_DOOR:
			case DARK_OAK_DOOR:
			case JUNGLE_DOOR:
			case SPRUCE_DOOR:
				isTallDoor = true;
		}
		return isTallDoor;
	}

	public static boolean isTrapDoor(Material mat) {
		boolean isTrapDoor = false;
		switch (mat) {
			case IRON_TRAPDOOR:
			case LEGACY_TRAP_DOOR:
				isTrapDoor = true;
		}
		return isTrapDoor;
	}

	public static boolean isSign(Material mat) {
		boolean isSign = false;
		switch (mat) {
			case LEGACY_WALL_SIGN:
			case LEGACY_SIGN_POST:
				isSign = true;
		}
		return isSign;
	}

	public static Set<Material> getSignMaterials() {
		Set<Material> mats = new HashSet<>();
		mats.add(Material.LEGACY_SIGN_POST);
		mats.add(Material.LEGACY_WALL_SIGN);
		return mats;
	}

	public static CompletableFuture<Set<Point>> getPointsConnected(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int maxReturns, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		return CompletableFuture.supplyAsync(() -> {
			List<Set<Point>> layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, -1, ignorePoints, searchablePoints, null, ConnectedThreshold.FACES).join();
			return flattenLayers(layers);
		});
	}

	public static CompletableFuture<Set<Point>> getPointsConnected(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		return CompletableFuture.supplyAsync(() -> {
			List<Set<Point>> layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, -1, rangeLimit, -1, ignorePoints, searchablePoints, null, ConnectedThreshold.FACES).join();
			return flattenLayers(layers);
		});
	}

	public static CompletableFuture<Set<Point>> getPointsConnected(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, ConnectedThreshold connectedThreshold) {
		return CompletableFuture.supplyAsync(() -> {
			List<Set<Point>> layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, -1, rangeLimit, -1, ignorePoints, null, null, connectedThreshold).join();
			return flattenLayers(layers);
		});
	}

	public static CompletableFuture<Set<Point>> getPointsConnected(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int rangeLimit, int layerLimit, Set<Point> ignorePoints, ConnectedThreshold connectedThreshold) {
		return CompletableFuture.supplyAsync(() -> {
			List<Set<Point>> layers = getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, -1, rangeLimit, layerLimit, ignorePoints, null, null, connectedThreshold).join();
			return flattenLayers(layers);
		});
	}

	public static CompletableFuture<List<Set<Point>>> getPointsConnectedAsLayers(World world, Point origin, int layerLimit, Set<Point> searchablePoints) {
		Set<Point> originLayer = new HashSet<>();
		originLayer.add(origin);
		int rangeLimit = layerLimit + 1;
		return getPointsConnectedAsLayers(world, origin, originLayer, null, null, -1, rangeLimit, layerLimit, null, searchablePoints, null, ConnectedThreshold.FACES);
	}

	public static CompletableFuture<List<Set<Point>>> getPointsConnectedAsLayers(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int maxReturns, int rangeLimit, Set<Point> ignorePoints, Map<Point, Material> pretendPoints) {
		if (originLayer == null) {
			originLayer = new HashSet<>();
			originLayer.add(origin);
		}
		return getPointsConnectedAsLayers(world, origin, originLayer, traverseMaterials, returnMaterials, maxReturns, rangeLimit, -1, ignorePoints, null, pretendPoints, ConnectedThreshold.FACES);
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
	public static CompletableFuture<List<Set<Point>>> getPointsConnectedAsLayers(World world, Point origin, Set<Point> originLayer, Set<Material> traverseMaterials, Set<Material> returnMaterials, int maxReturns, int rangeLimit, int layerLimit, Set<Point> ignorePoints, Set<Point> searchablePoints, Map<Point, Material> pretendPoints, ConnectedThreshold connectedThreshold) {
		//make ignorePoints default to empty
		if (ignorePoints == null)
			ignorePoints = new HashSet<>();
		final Set<Point> finalIgnorePoints = ignorePoints;

		if (pretendPoints == null)
			pretendPoints = new HashMap<>();
		final Map<Point, Material> finalPretendPoints = pretendPoints;

		return CompletableFuture.supplyAsync(() -> {
			List<Set<Point>> matchesAsLayers = new ArrayList<>();
			Set<Point> connected = new HashSet<>();

			Set<Point> visited = new HashSet<>(1000);
			Deque<Point> layer;
			Deque<Point> nextLayer = new ArrayDeque<>();
			int layerIndex = -1;
			Material mat;
			Point center;

			//fill nextLayer and visited from originLayer
			nextLayer.addAll(originLayer);
			visited.addAll(originLayer);

			int recursionLimit2Max = 10 * 6*(int)Math.pow(rangeLimit*2, 2);
			int recursionLimit = (int)Math.pow(rangeLimit/2, 3);
			long lastSleepEnd = System.currentTimeMillis();
			int matchCount = 0;
			int sleeplessCount = 0; //just for debugging
			while (!nextLayer.isEmpty()) {
				if (recursionLimit-- <= 0) {
					Debug.error("Wall recursionLimit exhausted");
					break;
				}

				layerIndex++;
				layer = nextLayer;
				nextLayer = new ArrayDeque<>();

				//process layer
				int recursionLimit2 = recursionLimit2Max;
				while (!layer.isEmpty()) {
					long elapsed = System.currentTimeMillis() - lastSleepEnd;
					if (elapsed > 15) { //use "> 15" except when debugging
//						Debug.msg("Sleeping after not sleeping " + sleeplessCount + " times.");
						Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS); //use "50"ms except when debugging
						lastSleepEnd = System.currentTimeMillis();
						sleeplessCount = 0;
					} else {
						sleeplessCount++;
					}

					if (recursionLimit2-- <= 0) {
						Debug.error("Wall recursionLimit2 exhausted");
						break;
					}

					center = layer.pop();
					connected.clear();

					//handle ConnectedThreshold.POINTS
					if (connectedThreshold == ConnectedThreshold.POINTS) {
						//iterate over the 27 (3*3*3) blocks around center
						for (int x = center.xInt()-1; x <= center.xInt()+1; x++) {
							for (int y = center.yInt()-1; y <= center.yInt()+1; y++) {
								for (int z = center.zInt()-1; z <= center.zInt()+1; z++) {
									connected.add(new Point(x, y, z));
								}
							}
						}
					}

					//handle ConnectedThreshold.FACES
					if (connectedThreshold == ConnectedThreshold.FACES) {
						//iterate over the 6 blocks adjacent to center
						connected.add(center.add(1, 0, 0));
						connected.add(center.add(-1, 0, 0));
						connected.add(center.add(0, 1, 0));
						connected.add(center.add(0, -1, 0));
						connected.add(center.add(0, 0, 1));
						connected.add(center.add(0, 0, -1));
					}

					//process connected points
					for (Point p : connected) {
						if (!visited.contains(p)) {
							visited.add(p);

							//ignore ignorePoints
							if (finalIgnorePoints.contains(p))
								continue;

							//ignore unsearchable points
							if (searchablePoints != null && !searchablePoints.contains(p)) {
								continue;
							}

							//ignore out of range points
							if (!isInRange(p, origin, rangeLimit))
								continue;

							if (finalPretendPoints.containsKey(p)) {
								mat = finalPretendPoints.get(p);
							} else {

								//trying to get block in unloaded chunk causes thread to hang and thus can crash server
								//so just consider such blocks to have material type of null
								boolean pInLoadedChunk = world.isChunkLoaded(p.xInt() >> 4, p.zInt() >> 4);
								mat = pInLoadedChunk?p.getBlock(world).getType():null;
								if (!pInLoadedChunk) Debug.msg("gpcal: FOUND UNLOADED POINT p: " + p); //TODO:: delete this line
							}

							//add to matchesAsLayers if it matches a returnMaterials type
							if (returnMaterials == null || returnMaterials.contains(mat)) {
								//"while" not "if" because maybe only matching blocks are far away but connected by wall
								while (layerIndex >= matchesAsLayers.size()) {
									matchesAsLayers.add(new HashSet<>());
								}
								matchesAsLayers.get(layerIndex).add(p);
								matchCount++;
							}

							//consider adding point to nextLayer
							if (traverseMaterials == null || traverseMaterials.contains(mat)) {
								nextLayer.push(p);
							}

							if (maxReturns != -1 && matchCount >= maxReturns) break;
						}
					} //end of for loop
					if (maxReturns != -1 && matchCount >= maxReturns) break;
				} //end of inner while
				if (maxReturns != -1 && matchCount >= maxReturns) break;

				if (layerLimit != -1 && matchesAsLayers.size() >= layerLimit) {
					matchesAsLayers = matchesAsLayers.subList(0, layerLimit); //enforce layerLimit (since multiple layers might have been added)
					break;
				}
			} //end of outer while

			return ImmutableList.copyOf(matchesAsLayers);
		});
	}

	private static boolean isInRange(Point p, Point origin, int rangeLimit) {
		boolean inRange = true;

		inRange = inRange && (Math.abs(p.xInt() - origin.xInt())) <= rangeLimit;
		inRange = inRange && (Math.abs(p.yInt() - origin.yInt())) <= rangeLimit;
		inRange = inRange && (Math.abs(p.zInt() - origin.zInt())) <= rangeLimit;

		return inRange;
	}
}