package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import java.util.ArrayList;

public class FortressGeneratorRunePattern {
	private Block anchorBlock;
	private ArrayList<Point> pointsInPattern = null;
	private Point redstoneWirePoint = null;

	public FortressGeneratorRunePattern(Block anchorBlock) {
		this.anchorBlock = anchorBlock;
	}

	public boolean matchesReadyPattern() {
		if (this.anchorBlock.getType() == Material.GOLD_BLOCK) {
			World world = this.anchorBlock.getWorld();
			Point a = new Point(this.anchorBlock.getLocation());

			//set towardFront, towardBack, towardLeft, towardRight
			Point signPoint = this.getSignPoint();
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
				matches = matches && p.matches(Material.DIAMOND_BLOCK);
				p.add(towardRight); //NE
				matches = matches && p.matches(Material.IRON_BLOCK);
				p.add(towardFront); //E
				matches = matches && p.matches(Material.CHEST);
				p.add(towardFront); //SE
				matches = matches && p.matches(Material.AIR);
				p.add(towardLeft); //S
				matches = matches && p.matches(Material.WALL_SIGN);
				p.add(towardLeft); //SW
				matches = matches && p.matches(Material.AIR);
				p.add(towardBack); //W
				matches = matches && p.matches(Material.REDSTONE_WIRE);
				p.add(towardBack); //NW
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

				Bukkit.broadcastMessage("matches: " + matches);
				return matches;
			} else {
				Bukkit.broadcastMessage("matches: false");
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean contains(Block block) {
		if (this.pointsInPattern == null) {
			this.pointsInPattern = new ArrayList<Point>();

			Point a = new Point(this.anchorBlock.getLocation());
			for (int y = -1; y <= 0; y++) {
				for (int x = -1; x <= 1; x++) {
					for (int z = -1; z <= 1; z++) {
						this.pointsInPattern.add(new Point(a.world, a.x + x, a.y + y, a.z + z));
					}
				}
			}
		}

		return this.pointsInPattern.contains(new Point(block.getLocation()));
	}

	public Block getAnchorBlock() {
		return this.anchorBlock;
	}

	public boolean setSignText(String line1) {
		return setSignText(line1, "", "");
	}
	public boolean setSignText(String line1, String line2) {
		return setSignText(line1, line2, "");
	}
	public boolean setSignText(String line1, String line2, String line3) {
		Point signPoint = this.getSignPoint();
		if (signPoint != null) {
			Block signBlock = signPoint.getBlock();
			if (signBlock != null) {
				Sign sign = (Sign)signBlock.getState();
				if (sign != null) {
					sign.setLine(0, "Fortress:");
					sign.setLine(1, line1);
					sign.setLine(2, line2);
					sign.setLine(3, line3);
					sign.update();
					return true;
				}
			}
		}
		return false;
	}

	private Point getSignPoint() {
		Point a = new Point(this.anchorBlock.getLocation());

		ArrayList<Point> points = new ArrayList<Point>();
		points.add(new Point(a.world, a.x + 1, a.y, a.z));
		points.add(new Point(a.world, a.x - 1, a.y, a.z));
		points.add(new Point(a.world, a.x, a.y, a.z + 1));
		points.add(new Point(a.world, a.x, a.y, a.z - 1));

		Point signPoint = null;
		for (Point p : points) {
			Block block = anchorBlock.getWorld().getBlockAt(p);
			if (block.getType() == Material.WALL_SIGN) {
				signPoint = p;
			}
		}

		return signPoint;
	}
}
