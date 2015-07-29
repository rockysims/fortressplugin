package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;

public class FortressGeneratorRunePattern implements Memorable {
	private ArrayList<Point> pointsInPattern = new ArrayList<Point>();
	private boolean matchedReadyPattern = false;
	public Point anchorPoint = null;
	public Point pausePoint = null;
	public Point runningPoint = null;
	public Point fuelPoint = null;
	public Point signPoint = null;

	public void saveTo(Memory m) {
		m.save("pointsInPattern", pointsInPattern);
		m.save("matchedReadyPattern", matchedReadyPattern);
		m.save("anchorPoint", anchorPoint);
		m.save("pausePoint", pausePoint);
		m.save("runningPoint", runningPoint);
		m.save("fuelPoint", fuelPoint);
		m.save("signPoint", signPoint);
	}

	public static FortressGeneratorRunePattern loadFrom(Memory m) {
		ArrayList<Point> pointsInPattern = m.loadPoints("pointsInPattern");
		boolean matchedReadyPattern = m.loadBoolean("matchedReadyPattern");
		FortressGeneratorRunePattern instance = new FortressGeneratorRunePattern(pointsInPattern, matchedReadyPattern);
		instance.anchorPoint = m.loadPoint("anchorPoint");
		instance.pausePoint = m.loadPoint("pausePoint");
		instance.runningPoint = m.loadPoint("runningPoint");
		instance.fuelPoint = m.loadPoint("fuelPoint");
		instance.signPoint = m.loadPoint("signPoint");
		return instance;
	}

	private FortressGeneratorRunePattern(ArrayList<Point> pointsInPattern, boolean matchedReadyPattern) {
		this.pointsInPattern = pointsInPattern;
		this.matchedReadyPattern = matchedReadyPattern;
	}

	//------------------------------------------------------------------------------------------------------------------

	public FortressGeneratorRunePattern(Block anchorBlock) {
		this.anchorPoint = new Point(anchorBlock.getLocation());
		this.runPatternMatch(anchorBlock);
	}

	public void runPatternMatch(Block anchorBlock) {
		if (anchorBlock != null && anchorBlock.getType() == Material.GOLD_BLOCK) {
			World world = anchorBlock.getWorld();
			Point a = new Point(anchorBlock.getLocation());

			//set towardFront, towardBack, towardLeft, towardRight
			Point signPoint = this.findSignPoint(world);
			if (signPoint != null) {
				Point towardFront = new Point(signPoint.subtract(a));
				Point towardLeft = new Point(world, towardFront.z, 0, towardFront.x);
				if (towardFront.x == 0) {
					towardLeft = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);
				}
				Point towardBack = new Point(world, -1 * towardFront.x, 0, -1 * towardFront.z);
				Point towardRight = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);

				boolean matches = true;
				Point p;

				//Layer 2 (top)
				p = new Point(a);
				matches = matches && p.matches(Material.GOLD_BLOCK);
				p.add(towardBack); //N
				this.runningPoint = new Point(p);
				matches = matches && p.matches(Material.DIAMOND_BLOCK);
				p.add(towardRight); //NE
				this.fuelPoint = new Point(p);
				matches = matches && p.matches(Material.IRON_BLOCK);
				p.add(towardFront); //E
				matches = matches && p.matches(Material.CHEST);
				p.add(towardFront); //SE
				matches = matches && p.matches(Material.AIR);
				p.add(towardLeft); //S
				this.signPoint = new Point(p);
				matches = matches && p.matches(Material.WALL_SIGN);
				p.add(towardLeft); //SW
				matches = matches && p.matches(Material.AIR);
				p.add(towardBack); //W
				matches = matches && p.matches(Material.REDSTONE_WIRE);
				p.add(towardBack); //NW
				this.pausePoint = new Point(p);
				matches = matches && p.matches(Material.IRON_BLOCK);


				//Layer 1 (bottom)
				p = new Point(world, a.x, a.y - 1, a.z);
				matches = matches && p.matches(Material.COBBLESTONE);
				p.add(towardBack); //N
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardRight); //NE
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardFront); //E
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardFront); //SE
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardLeft); //S
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardLeft); //SW
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardBack); //W
				matches = matches && p.matches(Material.OBSIDIAN);
				p.add(towardBack); //NW
				matches = matches && p.matches(Material.OBSIDIAN);

				//fill pointsInPattern
				for (int y = -1; y <= 0; y++) {
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							this.pointsInPattern.add(new Point(a.world, a.x + x, a.y + y, a.z + z));
						}
					}
				}

				Bukkit.broadcastMessage("matches: " + matches);
				this.matchedReadyPattern = matches;
			} else {
				this.matchedReadyPattern = false;
			}
		} else {
			this.matchedReadyPattern = false;
		}
	}
	private Point findSignPoint(World world) {
		Point a = this.anchorPoint;

		ArrayList<Point> points = new ArrayList<Point>();
		points.add(new Point(a.world, a.x + 1, a.y, a.z));
		points.add(new Point(a.world, a.x - 1, a.y, a.z));
		points.add(new Point(a.world, a.x, a.y, a.z + 1));
		points.add(new Point(a.world, a.x, a.y, a.z - 1));

		Point signPoint = null;
		for (Point p : points) {
			Block block = world.getBlockAt(p);
			if (block.getType() == Material.WALL_SIGN) {
				signPoint = p;
			}
		}

		return signPoint;
	}

	public boolean matchedReadyPattern() {
		return this.matchedReadyPattern;
	}

	public boolean contains(Block block) {
		return this.pointsInPattern.contains(new Point(block.getLocation()));
	}
}
