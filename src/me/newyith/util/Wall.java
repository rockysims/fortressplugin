package me.newyith.util;

import org.bukkit.Material;

import java.util.*;

public class Wall {
	public enum ConnectedThreshold {
		POINTS,
		//LINES,
		FACES
	};

	public static Set<Point> flattenLayers(List<List<Point>> layers) {
		Set<Point> points = new HashSet<>();

		for (List<Point> layer : layers) {
			for (Point p : layer) {
				points.add(p);
			}
		}

		return points;
	}

	public static List<List<Point>> merge(List<List<Point>> layers1, List<List<Point>> layers2) {
		List<List<Point>> layers = new ArrayList<>();

		int biggestSize = Math.max(layers1.size(), layers2.size());
		for (int i = 0; i < biggestSize; i++) {
			//process layer i
			layers.add(new ArrayList<>());
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

		points.add(new Point(p.world, p.x + 1, p.y, p.z));
		points.add(new Point(p.world, p.x - 1, p.y, p.z));
		points.add(new Point(p.world, p.x, p.y + 1, p.z));
		points.add(new Point(p.world, p.x, p.y - 1, p.z));
		points.add(new Point(p.world, p.x, p.y, p.z + 1));
		points.add(new Point(p.world, p.x, p.y, p.z - 1));

		return points;
	}

	public static boolean isDoor(Material mat) {
		boolean isDoor = false;
		switch (mat) {
			case IRON_DOOR_BLOCK:
			case WOODEN_DOOR:
			case ACACIA_DOOR:
			case BIRCH_DOOR:
			case DARK_OAK_DOOR:
			case JUNGLE_DOOR:
			case SPRUCE_DOOR:
			case TRAP_DOOR:
			case IRON_TRAPDOOR:
				isDoor = true;
		}
		return isDoor;
	}

	public static boolean isTrapDoor(Material mat) {
		boolean isTrapDoor = false;
		switch (mat) {
			case TRAP_DOOR:
			case IRON_TRAPDOOR:
				isTrapDoor = true;
		}
		return isTrapDoor;
	}

	public static boolean isSign(Material mat) {
		boolean isSign = false;
		switch (mat) {
			case WALL_SIGN:
			case SIGN_POST:
				isSign = true;
		}
		return isSign;
	}

	public static Set<Material> getSignMaterials() {
		Set<Material> mats = new HashSet<>();
		mats.add(Material.SIGN_POST);
		mats.add(Material.WALL_SIGN);
		return mats;
	}

	public static Set<Point> getPointsConnected(Point origin, Set<Point> originLayer, Set<Material> wallBlocks, Set<Material> returnBlocks, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints) {
		List<List<Point>> layers = getPointsConnectedAsLayers(origin, originLayer, wallBlocks, returnBlocks, rangeLimit, ignorePoints, searchablePoints, ConnectedThreshold.FACES);
		return flattenLayers(layers);
	}

	public static Set<Point> getPointsConnected(Point origin, Set<Point> originLayer, Set<Material> wallBlocks, Set<Material> returnBlocks, int rangeLimit, Set<Point> ignorePoints, ConnectedThreshold connectedThreshold) {
		List<List<Point>> layers = getPointsConnectedAsLayers(origin, originLayer, wallBlocks, returnBlocks, rangeLimit, ignorePoints, null, connectedThreshold);
		return flattenLayers(layers);
	}

	public static List<List<Point>> getPointsConnectedAsLayers(Point origin, Set<Point> originLayer, Set<Material> wallBlocks, Set<Material> returnBlocks, int rangeLimit, Set<Point> ignorePoints) {
		if (originLayer == null) {
			originLayer = new HashSet<>();
			originLayer.add(origin);
		}
		return getPointsConnectedAsLayers(origin, originLayer, wallBlocks, returnBlocks, rangeLimit, ignorePoints, null, ConnectedThreshold.FACES);
	}

	/**
	 * Looks at all blocks connected to the generator by wallMaterials (directly or recursively).
	 *
	 * @param origin The rangeLimit is calculated relative to this point.
	 * @param originLayer The first point(s) to search outward from.
	 * @param wallMaterials List of connecting block types. If null, all materials traversable.
	 * @param returnMaterials List of block types to look for and return when connected to the wall or null to return all block types.
	 * @param rangeLimit The maximum distance away from origin to search.
	 * @param ignorePoints When searching, these points will be ignored (not traversed or returned). If null, no points ignored.
	 * @param searchablePoints When searching, only these points will be visited (traversed and/or returned). If null, all points searchable.
	 * @param connectedThreshold Whether connected means 3x3x3 area or only the 6 blocks connected by faces.
	 * @return List of all points (blocks) connected to the originLayer by wallMaterials and matching a block type in returnMaterials.
	 */
	public static List<List<Point>> getPointsConnectedAsLayers(Point origin, Set<Point> originLayer, Set<Material> wallMaterials, Set<Material> returnMaterials, int rangeLimit, Set<Point> ignorePoints, Set<Point> searchablePoints, ConnectedThreshold connectedThreshold) {
		Debug.start("getPointsConnectedAsLayers() all");

		List<List<Point>> matchesAsLayers = new ArrayList<>();
		Set<Point> connected = new HashSet<>();

		Set<Point> visited = new HashSet<>(1000);
		Deque<Point> layer;
		Deque<Point> nextLayer = new ArrayDeque<>();
		int layerIndex = -1;
		Material m;
		Point center;

		//fill nextLayer and visited from originLayer
		nextLayer.addAll(originLayer);
		visited.addAll(originLayer);

		//make ignorePoints default to empty
		if (ignorePoints == null)
			ignorePoints = new HashSet<>();

		int recursionLimit2Max = 10 * 6*(int)Math.pow(rangeLimit*2, 2);
		int recursionLimit = (int)Math.pow(rangeLimit/2, 3);
		while (!nextLayer.isEmpty()) {
			if (recursionLimit-- <= 0) {
				Debug.error("Wall recursionLimit exhausted");
				break;
			}

			layerIndex++;
			layer = nextLayer;
			nextLayer = new ArrayDeque<>();

			//Debug.start("process layer");
			//Debug.msg("layer.size(): " + String.valueOf(layer.size()));

			//process layer
			int recursionLimit2 = recursionLimit2Max;
			while (!layer.isEmpty()) {
				//Debug.start("inner loop");

				if (recursionLimit2-- <= 0) {
					Debug.error("Wall recursionLimit2 exhausted");
					break;
				}

				//Debug.start("find connected points");

				center = layer.pop();
				connected.clear();

				//handle ConnectedThreshold.POINTS
				if (connectedThreshold == ConnectedThreshold.POINTS) {
					//iterate over the 27 (3*3*3) blocks around center
					for (int x = (int)center.x-1; x <= center.x+1; x++) {
						for (int y = (int)center.y-1; y <= center.y+1; y++) {
							for (int z = (int)center.z-1; z <= center.z+1; z++) {
								connected.add(new Point(center.world, x, y, z));
							}
						}
					}
				}

				//handle ConnectedThreshold.FACES
				if (connectedThreshold == ConnectedThreshold.FACES) {
					//iterate over the 6 blocks adjacent to center
					int x = (int)center.x;
					int y = (int)center.y;
					int z = (int)center.z;
					connected.add(new Point(center.world, x+1, y, z));
					connected.add(new Point(center.world, x-1, y, z));
					connected.add(new Point(center.world, x, y+1, z));
					connected.add(new Point(center.world, x, y-1, z));
					connected.add(new Point(center.world, x, y, z+1));
					connected.add(new Point(center.world, x, y, z-1));
				}

				//Debug.stop("find connected points");

				//Debug.start("process connected points");

				//process connected points
				for (Point p : connected) {
					if (!visited.contains(p)) {
						visited.add(p);

						//ignore ignorePoints
						if (ignorePoints.contains(p))
							continue;

						//ignore unsearchable points
						if (searchablePoints != null && !searchablePoints.contains(p)) {
							continue;
						}

						//ignore out of range points
						if (!isInRange(p, origin, rangeLimit))
							continue;

						m = p.getBlock().getType();

						//add to matchesAsLayers if it matches a returnMaterials type
						if (returnMaterials == null || returnMaterials.contains(m)) {
							//"while" not "if" because maybe only matching blocks are far away but connected by wall
							while (layerIndex >= matchesAsLayers.size()) {
								matchesAsLayers.add(new ArrayList<Point>());
							}
							matchesAsLayers.get(layerIndex).add(p);
						}

						//consider adding point to nextLayer
						if (wallMaterials == null || wallMaterials.contains(m)) {
							nextLayer.push(p);
						}
					}
				}

				//Debug.stop("process connected points");

				//Debug.stop("inner loop");

			}

			//Debug.stop("process layer");

		}

//		Debug.msg("Wall.getPointsConnected visited " + String.valueOf(visited.size()));
		//Debug.msg("Wall.getPointsConnected returning " + String.valueOf(matchesAsLayers.size()) + " matchesAsLayers");

//		Debug.end("getPointsConnectedAsLayers() all");

		return matchesAsLayers;
	}

	private static boolean isInRange(Point p, Point origin, int rangeLimit) {
		boolean inRange = true;

		inRange = inRange && (Math.abs(p.x - origin.x)) <= rangeLimit;
		inRange = inRange && (Math.abs(p.y - origin.y)) <= rangeLimit;
		inRange = inRange && (Math.abs(p.z - origin.z)) <= rangeLimit;

		return inRange;
	}
}