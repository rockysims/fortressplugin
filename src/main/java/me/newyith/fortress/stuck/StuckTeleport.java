package me.newyith.fortress.stuck;

import me.newyith.fortress.core.BaseCore;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Cuboid;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.stream.Collectors;

public class StuckTeleport {
	private Player player;
	private World world;
	private Point origin;

	private final int distBeyond = 10; //how far outside fortress to teleport

	public StuckTeleport(Player player, World world, Point origin) {
		this.player = player;
		this.world = world;
		this.origin = origin;
	}

	public static StuckTeleportResult teleport(Player player, String actionName) {
		World world = player.getWorld();
		Point origin = new Point(player.getLocation());
		return teleport(player, world, origin, actionName);
	}

	public static StuckTeleportResult teleport(Player player, World world, Point origin, String actionName) {
		StuckTeleport stuckTeleport = new StuckTeleport(player, world, origin);
//		Debug.start("stuckTeleport.execute()");
		StuckTeleportResult result = stuckTeleport.execute();
//		Debug.end("stuckTeleport.execute()");

		if (actionName != null) {
			if (result == StuckTeleportResult.NO_VALID_DESTINATION) {
				player.sendMessage(ChatColor.AQUA + actionName + " failed because no suitable destination was found.");
			} else if (result == StuckTeleportResult.NO_NEARBY_CORES) {
				player.sendMessage(ChatColor.AQUA + actionName + " failed because no nearby fortress was found.");
			}
		}

		return result;
	}

	private StuckTeleportResult execute() {
		StuckTeleportResult result;

		int radius = FortressPlugin.config_generationRangeLimit * 3 + distBeyond; //the '* 3' is to get diameter + radius
		Set<BaseCore> coresInRadius = FortressesManager.forWorld(world).getCoresInRadius(origin, radius);

		Set<Cuboid> coreCuboidsContainingOrigin = coresInRadius.stream()
				.map(BaseCore::buildCuboid)
				.filter(cuboid -> cuboid.contains(origin))
				.collect(Collectors.toSet());

		if (coreCuboidsContainingOrigin.isEmpty()) {
			result = StuckTeleportResult.NO_NEARBY_CORES;
		} else {
			Set<Point> insidePoints = buildPointsInside(coresInRadius);

			boolean teleported = false;
			List<Point> nearbyPoints = getRandomNearbyPointsAtOriginHeight(50, coreCuboidsContainingOrigin);
			double tryCount = 0;
			double maxCount = nearbyPoints.size();
			double sqrtDeltaLimitMax = Math.sqrt(world.getMaxHeight());
			for (Point p : nearbyPoints) {
				tryCount++;
				int deltaLimit = 8 + (int)Math.pow(sqrtDeltaLimitMax*(tryCount / maxCount), 2);
				p = findValidTeleportDest(p, insidePoints, deltaLimit);
				if (p != null) {
					p = p.add(0.5F, 0, 0.5F);
					teleportAndFaceOrigin(p);
					teleported = true;
					break;
				}
			}

			if (teleported) {
				result = StuckTeleportResult.SUCCESS;
			} else {
				result = StuckTeleportResult.NO_VALID_DESTINATION;
			}
		}

		return result;
	}

	private List<Point> getRandomNearbyPointsAtOriginHeight(int limit, Set<Cuboid> coreCuboidsContainingOrigin) {
		int y = origin.yInt();
		Cuboid combinedCuboid = Cuboid.fromCuboids(coreCuboidsContainingOrigin, world);
		Set<Point> outerSheet = combinedCuboid.grow(distBeyond).getPointsAtHeight(y);

		final Set<Point> nearbyPoints = new HashSet<>();
		nearbyPoints.addAll(outerSheet);
		coreCuboidsContainingOrigin.forEach(coreCuboid -> {
			nearbyPoints.removeAll(coreCuboid.getPointsAtHeight(y));
		});

		//if distBeyond is small, we may need to add back some outerSheet points
		for (Point pOuter : outerSheet) {
			if (nearbyPoints.size() >= limit) break;
			else nearbyPoints.add(pOuter);
		}

		List<Point> nearbyPointsList = new ArrayList<>(nearbyPoints);
		Collections.shuffle(nearbyPointsList);

		//return first limit points in nearbyPoints
		return new ArrayList<>(nearbyPointsList.subList(0, Math.min(limit, nearbyPoints.size() - 1))); //creating new list allows garbage collection of old list
	}

	private Point findValidTeleportDest(Point start, Set<Point> insidePoints, int maxDelta) {
		int minDestHeight = 3; //layer below highest floor bedrock layer
		int maxDestHeight = (isNether())
			? 123 //lowest ceiling bedrock layer
			: world.getMaxHeight();

		//find closest solid block at p.x, p.z with two airy blocks immediately above it (return point above it)
		for (int delta = 0; delta <= maxDelta; delta++) {
			Point up = start.add(0, delta, 0);
			if (up.yInt() <= maxDestHeight && !insidePoints.contains(up) && isValidDest(up)) return up;

			Point down = start.add(0, -1 * delta, 0);
			if (down.yInt() >= minDestHeight && !insidePoints.contains(down) && isValidDest(down)) return down;
		}

		return null;
	}

	private boolean isValidDest(Point dest) {
		if (Blocks.isAiry(dest, world)) { //dest is airy
			Point belowDest = dest.add(0, -1, 0);
			Material belowDestMat = belowDest.getType(world);
			if (belowDestMat.isSolid() && belowDestMat.isOccluding()) { //belowDest is solid and not see through (trap doors, signs, etc. are see through)
				Point aboveDest = dest.add(0, 1, 0);
				if (Blocks.isAiry(aboveDest, world)) {
					return true; //belowDest solid, dest airy, aboveDest airy
				}
			}
		}
		return false;
	}

	private void teleportAndFaceOrigin(Point target) {
		Location originLoc = origin.toLocation(world);
		Location targetLoc = target.toLocation(world);
		targetLoc = faceLocationToward(targetLoc, originLoc);
		player.teleport(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	//bergerkiller's method lookAt() (https://bukkit.org/threads/lookat-and-move-functions.26768/)
	private Location faceLocationToward(Location loc, Location lookAt) {
		//Clone the loc to prevent applied changes to the input loc
		loc = loc.clone();

		// Values of change in distance (make it relative)
		double dx = lookAt.getX() - loc.getX();
		double dy = lookAt.getY() - loc.getY();
		double dz = lookAt.getZ() - loc.getZ();

		// Set yaw
		if (dx != 0) {
			// Set yaw start value based on dx
			if (dx < 0) {
				loc.setYaw((float) (1.5 * Math.PI));
			} else {
				loc.setYaw((float) (0.5 * Math.PI));
			}
			loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
		} else if (dz < 0) {
			loc.setYaw((float) Math.PI);
		}

		// Get the distance from dx/dz
		double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

		// Set pitch
		loc.setPitch((float) -Math.atan(dy / dxz));

		// Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
		loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
		loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);

		return loc;
	}


	private Set<Point> buildPointsInside(Set<BaseCore> cores) {
		Set<Point> pointsInside = new HashSet<>();
		for (BaseCore core : cores) {
			pointsInside.addAll(core.getPointsInsideFortress());
		}

		return pointsInside;
	}

	private boolean isNether() {
		return world.getName().equals("world_nether");
	}
}
