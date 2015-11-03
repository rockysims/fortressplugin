package me.newyith.fortress.generator.rune;

import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Sign;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class GeneratorRunePattern {
	private static class Model {
		private transient Set<Point> pointsInPattern = null;
		private transient World world = null;
		private String worldName = "";
		public Point anchorPoint = null;
		public Point pausePoint = null;
		public Point runningPoint = null;
		public Point fuelPoint = null;
		public Point signPoint = null;
		public Point chestPoint = null;
		public Point wirePoint = null;

		public Model(World world, Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
			this.worldName = world.getName();
			signPoint = s;
			wirePoint = w;
			anchorPoint = a;
			chestPoint = c;
			pausePoint = p;
			runningPoint = r;
			fuelPoint = f;
			onLoaded();
		}

		private void onLoaded() {
			//rebuild transient fields
			pointsInPattern = new HashSet<>();
			pointsInPattern.add(signPoint);
			pointsInPattern.add(wirePoint);
			pointsInPattern.add(anchorPoint);
			pointsInPattern.add(chestPoint);
			pointsInPattern.add(pausePoint);
			pointsInPattern.add(runningPoint);
			pointsInPattern.add(fuelPoint);
			world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonProperty("model")
	private void setModel(Model model) {
		this.model = model;
		model.onLoaded();
	}

	public GeneratorRunePattern(World world, Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
		model = new Model(world, s, w, a, c, p, r, f);
	}

	//-----------------------------------------------------------------------

	public World getWorld() {
		return model.world;
	}

	public Set<Point> getPoints() {
		return model.pointsInPattern;
	}

	public Point getAnchor() {
		return model.anchorPoint;
	}

	public boolean contains(Block block) {
		return model.pointsInPattern.contains(new Point(block));
	}

	public static GeneratorRunePattern tryReadyPattern(Block signBlock) {
		GeneratorRunePattern pattern = null;

		//if (found sign)
		if (signBlock.getType() == Material.WALL_SIGN) {
			World world = signBlock.getWorld();
			Point s = new Point(signBlock.getLocation());
			Point a = getPointSignAttachedTo(signBlock);

			//if (found anchor)
			if (a.is(Material.GOLD_BLOCK, world)) {
				//set towardFront, towardBack, towardLeft, towardRight
				Point towardFront = s.difference(a);
				Point towardLeft = new Point(towardFront.z(), 0, towardFront.x());
				if (towardFront.x() == 0) {
					towardLeft = new Point(-1 * towardLeft.x(), 0, -1 * towardLeft.z());
				}
				Point towardBack = new Point(-1 * towardFront.x(), 0, -1 * towardFront.z());
				Point towardRight = new Point(-1 * towardLeft.x(), 0, -1 * towardLeft.z());

				//find remaining points
				Point w = a.add(towardLeft);
				Point c = a.add(towardRight);
				Point p = a.add(0, -1, 0).add(towardLeft);
				Point r = a.add(0, -1, 0);
				Point f = a.add(0, -1, 0).add(towardRight);
				if (c.is(Material.REDSTONE_WIRE, world) && w.is(Material.CHEST, world)) {
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
				valid = valid && w.is(Material.REDSTONE_WIRE, world);
				valid = valid && c.is(Material.CHEST, world);
				valid = valid && p.is(Material.IRON_BLOCK, world);
				valid = valid && r.is(Material.DIAMOND_BLOCK, world);
				valid = valid && f.is(Material.IRON_BLOCK, world);

				if (valid) {
					pattern = new GeneratorRunePattern(world, s, w, a, c, p, r, f);
				}
			}
		}

		return pattern;
	}

	public static Point getPointSignAttachedTo(Block signBlock) {
		Point s = new Point(signBlock);
		Sign sign = (Sign) signBlock.getState().getData();
		BlockFace af = sign.getAttachedFace();
		int x = af.getModX();
		int y = af.getModY();
		int z = af.getModZ();
		return new Point(s.x() + x, s.y() + y, s.z() + z);
	}

	public boolean isValid() { //TODO: FortressesManager should call this during load and destroy invalid generators
		boolean valid = true;
		valid = valid && model.signPoint.is(Material.WALL_SIGN, model.world);
		valid = valid && model.anchorPoint.is(Material.DIAMOND_BLOCK, model.world);
		valid = valid && model.wirePoint.is(Material.REDSTONE_WIRE, model.world);
		valid = valid && model.chestPoint.is(Material.CHEST, model.world);

		int goldCount = 0;
		int ironCount = 0;
		Set<Point> points = new HashSet<>();
		points.add(model.pausePoint);
		points.add(model.runningPoint);
		points.add(model.fuelPoint);
		for (Point p : points) {
			if (model.pausePoint.is(Material.IRON_BLOCK, model.world)) {
				ironCount++;
			} else if (model.pausePoint.is(Material.GOLD_BLOCK, model.world)) {
				goldCount++;
			}
		}
		valid = valid && goldCount == 1;
		valid = valid && ironCount == 2;
		return valid;
	}
}
