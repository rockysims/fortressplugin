package me.newyith.command;

import me.newyith.generator.FortressGeneratorRune;
import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
import me.newyith.util.Cuboid;
import me.newyith.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class StuckPlayer {
	private Player player;
	private Point startPoint;
	private double maxHealthOnRecord;
	private long startTimestamp;

	private Map<Integer, String> messages;

	private final int nearbyRange = 10;
	private final int stuckDelayMs = FortressPlugin.config_stuckDelayMs;
	private final int cancelDistance = FortressPlugin.config_stuckCancelDistance;
	private Set<FortressGeneratorRune> nearbyRunes;

	public StuckPlayer(Player player) {
		this.player = player;
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

		nearbyRunes = FortressGeneratorRunesManager.getOtherRunesInRange(startPoint, FortressPlugin.config_generationRange);
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
		double changeInX = Math.abs(p.x - startPoint.x);
		double changeInY = Math.abs(p.y - startPoint.y);
		double changeInZ = Math.abs(p.z - startPoint.z);

		if (nearbyRunes.size() == 0) {
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
		Point p;
		boolean teleported = false;
		int attemptLimit = 20;
		while (!teleported && attemptLimit > 0) {
			attemptLimit--;

			p = getRandomNearbyPoint(startPoint);
			p = getValidTeleportDest(p);
			if (p != null) {
				p.x += 0.5F;
				p.z += 0.5F;
				player.teleport(p.toLocation());
				teleported = true;
			}
		}
		if (!teleported) {
			this.sendMessage("/stuck failed because no suitable destination was found.");
		}
	}

	private Point getValidTeleportDest(Point p) {
		Point validDest = null;

		if (p != null) {
			int maxHeight = p.world.getMaxHeight();
			for (int y = maxHeight-2; y >= 0; y--) {
				p.y = y;
				if (!p.is(Material.AIR)) {
					//first non air block

					//check if valid teleport destination
					if (p.getBlock().getType().isSolid()) {
						p.y++;
						validDest = p;
					}

					break;
				}
			}
		}

		return validDest;
	}

	private Point getRandomNearbyPoint(Point p) {
		Point nearbyPoint = null;

		if (nearbyRunes.size() > 0) {
			//set combinedCuboid
			List<Cuboid> runeCuboids = new ArrayList<>();
			nearbyRunes.stream().forEach(nearbyRune -> {
				runeCuboids.add(nearbyRune.getFortressCuboid());
			});
			Cuboid combinedCuboid = Cuboid.fromCuboids(runeCuboids, p.world);

			//set outerCuboid
			Point outerMin = combinedCuboid.getMin().add(-1 * nearbyRange, -1 * nearbyRange, -1 * nearbyRange);
			Point outerMax = combinedCuboid.getMax().add(nearbyRange, nearbyRange, nearbyRange);
			Cuboid outerCuboid = new Cuboid(outerMin, outerMax);

			//set combinedSheet and outerSheet
			double y = combinedCuboid.getMax().y;
			Set<Point> combinedSheet = combinedCuboid.getPointsAtHeight(y);
			Set<Point> outerSheet = outerCuboid.getPointsAtHeight(y);

			//set nearbyPoints
			List<Point> nearbyPoints = new ArrayList<>();
			nearbyPoints.addAll(outerSheet);
			nearbyPoints.removeAll(combinedSheet);

			if (nearbyPoints.size() > 0) {
				Collections.shuffle(nearbyPoints);
				nearbyPoint = nearbyPoints.get(0);
			}
		}

		return nearbyPoint;
	}
}
