package me.newyith.fortress.generator2.rune;

import me.newyith.fortress.memory.AbstractMemory;
import me.newyith.fortress.memory.Memorable;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Sign;

import java.util.ArrayList;
import java.util.List;

public class GeneratorRunePattern implements Memorable {
	private List<Point> pointsInPattern = new ArrayList<>();
	private boolean matchedReadyPattern = false;
	public Point anchorPoint = null;
	public Point pausePoint = null;
	public Point runningPoint = null;
	public Point fuelPoint = null;
	public Point signPoint = null;
	public Point chestPoint = null;
	public Point wirePoint = null;

	public void saveTo(AbstractMemory<?> m) {
		m.savePointListCompact("pointsInPattern", pointsInPattern);
		m.save("matchedReadyPattern", matchedReadyPattern);
		m.save("anchorPoint", anchorPoint);
		m.save("pausePoint", pausePoint);
		m.save("runningPoint", runningPoint);
		m.save("fuelPoint", fuelPoint);
		m.save("signPoint", signPoint);
		m.save("chestPoint", chestPoint);
		m.save("wirePoint", wirePoint);
	}

	public static GeneratorRunePattern loadFrom(AbstractMemory<?> m) {
		List<Point> pointsInPattern = m.loadPointListCompact("pointsInPattern");
		boolean matchedReadyPattern = m.loadBoolean("matchedReadyPattern");
		GeneratorRunePattern instance = new GeneratorRunePattern(pointsInPattern, matchedReadyPattern);
		instance.anchorPoint = m.loadPoint("anchorPoint");
		instance.pausePoint = m.loadPoint("pausePoint");
		instance.runningPoint = m.loadPoint("runningPoint");
		instance.fuelPoint = m.loadPoint("fuelPoint");
		instance.signPoint = m.loadPoint("signPoint");
		instance.chestPoint = m.loadPoint("chestPoint");
		instance.wirePoint = m.loadPoint("wirePoint");
		return instance;
	}

	private GeneratorRunePattern(List<Point> pointsInPattern, boolean matchedReadyPattern) {
		this.pointsInPattern = pointsInPattern;
		this.matchedReadyPattern = matchedReadyPattern;
	}

	//------------------------------------------------------------------------------------------------------------------

	public static Point getPointSignAttachedTo(Block signBlock) {
		Point s = new Point(signBlock.getLocation());
		Sign sign = (Sign) signBlock.getState().getData();
		BlockFace af = sign.getAttachedFace();
		int x = af.getModX();
		int y = af.getModY();
		int z = af.getModZ();
		return new Point(s.world, s.x + x, s.y + y, s.z + z);
	}

	public static GeneratorRunePattern tryPatternAt(Block signBlock) {
		GeneratorRunePattern pattern = null;

		//if (found sign)
		if (signBlock.getType() == Material.WALL_SIGN) {
			Point s = new Point(signBlock.getLocation());
			Point a = getPointSignAttachedTo(signBlock);

			//if (found anchor)
			if (a.is(Material.GOLD_BLOCK)) {
				World world = a.world;

				//set towardFront, towardBack, towardLeft, towardRight
				Point towardFront = s.difference(a);
				Point towardLeft = new Point(world, towardFront.z, 0, towardFront.x);
				if (towardFront.x == 0) {
					towardLeft = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);
				}
				Point towardBack = new Point(world, -1 * towardFront.x, 0, -1 * towardFront.z);
				Point towardRight = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);

				//find remaining points
				Point w = a.add(towardLeft);
				Point c = a.add(towardRight);
				Point p = a.add(0, -1, 0).add(towardLeft);
				Point r = a.add(0, -1, 0);
				Point f = a.add(0, -1, 0).add(towardRight);
				if (c.is(Material.REDSTONE_WIRE) && w.is(Material.CHEST)) {
					Point t;
					//reverse wire / chest
					t = w;
					w = c;
					c = t;
					//reverse pause / fuel
					t = p;
					p = f;
					f = t;
				}

				//check other blocks match pattern
				boolean valid = true;
				valid = valid && w.is(Material.REDSTONE_WIRE);
				valid = valid && c.is(Material.CHEST);
				valid = valid && p.is(Material.IRON_BLOCK);
				valid = valid && r.is(Material.DIAMOND_BLOCK);
				valid = valid && f.is(Material.IRON_BLOCK);

				if (valid) {
					pattern = new GeneratorRunePattern(s, w, a, c, p, r, f);
				}
			}
		}

		return pattern;
	}

	private GeneratorRunePattern(Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
		signPoint = s;
		wirePoint = w;
		anchorPoint = a;
		chestPoint = c;
		pausePoint = p;
		runningPoint = r;
		fuelPoint = f;

		//fill pointsInPattern
		this.pointsInPattern.add(s);
		this.pointsInPattern.add(w);
		this.pointsInPattern.add(a);
		this.pointsInPattern.add(c);
		this.pointsInPattern.add(p);
		this.pointsInPattern.add(r);
		this.pointsInPattern.add(f);
	}

	public boolean contains(Block block) {
		return this.pointsInPattern.contains(new Point(block.getLocation()));
	}

	public List<Point> getPoints() {
		return this.pointsInPattern;
	}
}
