package me.newyith.fortressOrig.command;

import me.newyith.fortressOrig.rune.generator.GeneratorRune;
import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.main.FortressesManager;
import me.newyith.fortressOrig.util.Cuboid;
import me.newyith.fortressOrig.util.Point;
import me.newyith.fortressOrig.util.Blocks;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class StuckPlayer {
	private Player player;
	private World world;
	private Point startPoint;
	private double maxHealthOnRecord;
	private long startTimestamp;

	private Map<Integer, String> messages;

	private final int distBeyondFortress = 10; //how far outside combinedCuboid to teleport
	private final int stuckDelayMs = FortressPlugin.config_stuckDelayMs;
	private final int cancelDistance = FortressPlugin.config_stuckCancelDistance;
	private Set<GeneratorRune> nearbyGeneratorRunes;

	public StuckPlayer(Player player) {
		this.player = player;
		this.world = player.getWorld();
		this.startPoint = new Point(player.getLocation());
		this.maxHealthOnRecord = player.getHealth();
		this.startTimestamp = new Date().getTime();

		this.messages = new HashMap<>();
		int ms;
		ms = 1*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 2*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 3*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 4*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 5*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 10*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 15*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 30*1000;
		this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.");
		ms = 1*60*1000;
		this.messages.put(ms, "/stuck teleport in 1 minute.");
		for (int i = 2; i*60*1000 < this.stuckDelayMs; i++) {
			ms = i*60*1000;
			this.messages.put(ms, "/stuck teleport in " + String.valueOf(ms/(1000*60)) + " minutes.");
		}

		//remove messages that would be shown later than or at stuckDelayMs
		List<Integer> displayTimes = new ArrayList<>(this.messages.keySet());
		for (int displayTime : displayTimes) {
			if (displayTime >= this.stuckDelayMs) {
				this.messages.remove(displayTime);
			}
		}

		nearbyGeneratorRunes = FortressesManager.forWorld(world).getGeneratorRunesNear(startPoint);
	}

	public void considerSendingMessage() {
		int remaining = this.getRemainingMs();

		List<Integer> displayTimes = new ArrayList<>(this.messages.keySet());
		Collections.sort(displayTimes);
		Collections.reverse(displayTimes);
		for (int displayTime : displayTimes) {
			if (remaining <= displayTime) {
				//time to display the message
				String msg = this.messages.get(displayTime);
				this.messages.remove(displayTime);
				sendMessage(msg);
				break;
			}
		}
	}

	public boolean considerCancelling() {
		boolean cancel = false;

		Point p = new Point(player.getLocation());
		double changeInX = Math.abs(p.x() - startPoint.x());
		double changeInY = Math.abs(p.y() - startPoint.y());
		double changeInZ = Math.abs(p.z() - startPoint.z());

		if (nearbyGeneratorRunes.size() == 0) {
			//player is too far from any fortress
			cancel = true;
			sendMessage("/stuck failed because you are too far from any fortress.");
		}

		if (changeInX > cancelDistance || changeInY > cancelDistance || changeInZ > cancelDistance) {
			//player moved too far away
			cancel = true;
			sendMessage("/stuck cancelled because you moved too far away.");
		}

		double health = this.player.getHealth();
		if (health < this.maxHealthOnRecord) {
			//player took damage
			cancel = true;
			sendMessage("/stuck cancelled because you took damage.");
		} else if (health > this.maxHealthOnRecord) {
			//player healed
			this.maxHealthOnRecord = this.player.getHealth();
		}

		return cancel;
	}

	public void sendStartMessage() {
		String msgLine1 = "/stuck will cancel if you move " + cancelDistance + "+ blocks away or take damage.";

		String msgLine2 = "";
		int ms = this.stuckDelayMs;
		if (ms <= 5*1000) {
			//first natural message will be soon enough
		} else if (ms < 60*1000) { //less than a minute delay
			msgLine2 = "/stuck teleport in " + String.valueOf(ms/1000) + " seconds.";
		} else {
			msgLine2 = "/stuck teleport in " + String.valueOf(ms/(1000*60)) + " minutes.";
		}

		this.sendMessage(msgLine1);
		if (msgLine2.length() > 0) {
			this.sendMessage(msgLine2);
		}
	}

	public void sendBePatientMessage() {
		int remainingSeconds = this.getRemainingMs() / 1000;
		String msg = "/stuck teleport in " + String.valueOf(remainingSeconds) + " seconds... be patient.";
		this.sendMessage(msg);
	}

	private void sendMessage(String msg) {
		msg = ChatColor.AQUA + msg;
		player.sendMessage(msg);
	}

	public boolean isPlayer(Player otherPlayer) {
		return player.getPlayer().getUniqueId() == otherPlayer.getPlayer().getUniqueId();
	}

	public boolean isDoneWaiting() {
		return this.getElapsedMs() > this.stuckDelayMs;
	}

	private int getElapsedMs() {
		long now = new Date().getTime();
		int elapsed = (int) (now - this.startTimestamp);
		return elapsed;
	}

	private int getRemainingMs() {
		return this.stuckDelayMs - this.getElapsedMs();
	}

	public void stuckTeleport() {
		boolean teleported = false;
		int attemptLimit = 50;

		List<Point> nearbyPoints = getRandomNearbyPoints(attemptLimit);
		for (Point p : nearbyPoints) {
			p = getValidTeleportDest(world, p);
			if (p != null) {
				p = p.add(0.5F, 0, 0.5F);
				teleportPlayer(player, p);
				teleported = true;
				break;
			}
		}

		if (!teleported) {
			//fallback to player's bed
			Location bedLoc = player.getBedSpawnLocation();
			if (bedLoc != null) {
				player.teleport(bedLoc);
				teleported = true;
			}
		}

		if (!teleported) {
			this.sendMessage("/stuck failed because no suitable destination was found.");
		}
	}

	private List<Point> getRandomNearbyPoints(int limit) {
		List<Point> nearbyPoints = new ArrayList<>();

		if (nearbyGeneratorRunes.size() > 0) {
			//set combinedCuboid (cuboid enclosing all nearby generators)
			List<Cuboid> runeCuboids = new ArrayList<>();
			nearbyGeneratorRunes.stream().forEach(nearbyRune -> {
				runeCuboids.add(nearbyRune.getFortressCuboid());
			});
			Cuboid combinedCuboid = Cuboid.fromCuboids(runeCuboids, world);

			//set outerCuboid (same as combinedCuboid except distBeyondFortress bigger in all directions)
			Point outerMin = combinedCuboid.getMin().add(-1 * distBeyondFortress, -1 * distBeyondFortress, -1 * distBeyondFortress);
			Point outerMax = combinedCuboid.getMax().add(distBeyondFortress, distBeyondFortress, distBeyondFortress);
			Cuboid outerCuboid = new Cuboid(outerMin, outerMax, world);

			//set combinedSheet and outerSheet
			double y = combinedCuboid.getMax().y();
			Set<Point> combinedSheet = combinedCuboid.getPointsAtHeight(y);
			Set<Point> outerSheet = outerCuboid.getPointsAtHeight(y);

			//set nearbyPoints
			nearbyPoints.addAll(outerSheet);
			nearbyPoints.removeAll(combinedSheet);
			Collections.shuffle(nearbyPoints);

			//nearbyPoints = first limit points in nearbyPoints
			nearbyPoints = new ArrayList<>(nearbyPoints.subList(0, limit)); //creating new list allows garbage collection of old list
		}

		return nearbyPoints;
	}

	// static //

	private static void teleportPlayer(Player player, Point target) {
		World world = player.getWorld();
		Location playerLoc = player.getLocation();
		Location targetLoc = target.toLocation(world);
		targetLoc = faceLocationToward(targetLoc, playerLoc);
		player.teleport(targetLoc);
	}

	public static boolean teleport(Player player) {
		World world = player.getWorld();
		Point playerPoint = new Point(player.getLocation());

		int radius = 16;
		boolean teleported = false;
		int attemptLimit = 50;
		List<Point> nearbyPoints = getNearbyPoints(world, playerPoint, radius, attemptLimit);
		for (Point p : nearbyPoints) {
			p = getValidTeleportDest(world, p);
			if (p != null) {
				p = p.add(0.5F, 0, 0.5F);
				teleportPlayer(player, p);
				teleported = true;
				break;
			}
		}

		if (!teleported) {
			//fallback to player's bed
			Location bedLoc = player.getBedSpawnLocation();
			if (bedLoc != null) {
				player.teleport(bedLoc);
				teleported = true;
			}
		}

		return teleported;
	}

	private static List<Point> getNearbyPoints(World world, Point center, int radius, int attemptLimit) {
		int r = radius;
		Point a = center.add(r, r, r);
		Point b = center.add(-1 * r, -1 * r, -1 * r);
		Cuboid cuboid = new Cuboid(a, b, world);
		List<Point> nearbyPoints = new ArrayList<>(cuboid.getPointsAtHeight(a.y()));
		Collections.shuffle(nearbyPoints);
		//nearbyPoints = first attemptLimit points in nearbyPoints
		nearbyPoints = new ArrayList<>(nearbyPoints.subList(0, attemptLimit)); //creating new list allows garbage collection of old list

		return nearbyPoints;
	}

	private static Point getValidTeleportDest(World world, Point p) {
		Point validDest = null;

		if (p != null) {
			//p = highest non air block at p.x, p.z
			int maxHeight = world.getMaxHeight();
			for (int y = maxHeight-2; y >= 0; y--) {
				p = new Point(p.xInt(), y, p.zInt());
				if (!Blocks.isAiry(p, world)) {
					//first non airy block
					break;
				}
			}

			//check if valid teleport destination
			if (p.getBlock(world).getType().isSolid()) {
				Point dest = p.add(0, 1, 0);
				Point aboveDest = dest.add(0, 1, 0);
				if (Blocks.isAiry(dest, world) && Blocks.isAiry(aboveDest, world)) {
					validDest = dest;
				}
			}
		}

		return validDest;
	}

	//bergerkiller's method lookAt() (https://bukkit.org/threads/lookat-and-move-functions.26768/)
	public static Location faceLocationToward(Location loc, Location lookat) {
		//Clone the loc to prevent applied changes to the input loc
		loc = loc.clone();

		// Values of change in distance (make it relative)
		double dx = lookat.getX() - loc.getX();
		double dy = lookat.getY() - loc.getY();
		double dz = lookat.getZ() - loc.getZ();

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
}