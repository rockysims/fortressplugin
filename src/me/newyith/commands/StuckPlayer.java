package me.newyith.commands;

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

	Random random = new Random();
	private int quadrantSize = 32;
	private final int stuckDelayMs = 30 * 1000;
	private final int cancelDistance = 4; //4 blocks

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

		if (changeInX > cancelDistance || changeInY > cancelDistance || changeInZ > cancelDistance) {
			//player moved too far away
			cancel = true;
			String msg = "/stuck cancelled because you moved too far away.";
			sendMessage(msg);
		}

		double health = this.player.getHealth();
		if (health < this.maxHealthOnRecord) {
			//player took damage
			cancel = true;
			String msg = "/stuck cancelled because you took damage.";
			sendMessage(msg);
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

	private Point getValidTeleportDest(Point center) {
		Point validDest = null;

		int maxHeight = center.world.getMaxHeight();
		Point p = new Point(center);
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

		return validDest;
	}

	private Point getRandomNearbyPoint(Point p) {
		//TODO: consider rewriting this in a way that makes more sense to read (still not sure it works correctly)

		int dist = quadrantSize  + quadrantSize / 2 + (int)(random.nextFloat() * quadrantSize);
		double x = p.x;
		double y = p.y;
		double z = p.z;


		//pick a quadrant
		//move left, right, forward, or backward by dist
		float f = random.nextFloat() * 100;
		if (f < 25) {
			x += dist;
		} else if (f < 50) {
			x -= dist;
		} else if (f < 75) {
			z += dist;
		} else {
			z -= dist;
		}
		//move left or right OR forward or backward by quadrantSize
		if (f < 50) { //x changed
			if (random.nextFloat() * 100 < 50) {
				z += quadrantSize;
			} else {
				z -= quadrantSize;
			}
		} else {// z changed
			if (random.nextFloat() * 100 < 50) {
				x += quadrantSize;
			} else {
				x -= quadrantSize;
			}
		}

		//move to random point in quadrant
		x += (int)(random.nextFloat() * quadrantSize) - (quadrantSize/2);
		z += (int)(random.nextFloat() * quadrantSize) - (quadrantSize/2);

		return new Point(p.world, x, y, z);
	}

}
