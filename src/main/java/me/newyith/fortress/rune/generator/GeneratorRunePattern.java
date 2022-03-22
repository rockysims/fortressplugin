package me.newyith.fortress.rune.generator;

import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Sign;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

//fully written again
public class GeneratorRunePattern {
	private static class Model {
		private transient Set<Point> pointsInPattern = null;
		private transient World world = null;
		private String worldName = "";
		private Point signPoint = null;
		private Point wirePoint = null;
		private Point anchorPoint = null;
		private Point chestPoint = null;
		private Point pausePoint = null;
		private Point runningPoint = null;
		private Point fuelPoint = null;

		@JsonCreator
		public Model(@JsonProperty("worldName")  String worldName,
					 @JsonProperty("signPoint") Point signPoint,
					 @JsonProperty("wirePoint") Point wirePoint,
					 @JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("chestPoint") Point chestPoint,
					 @JsonProperty("pausePoint") Point pausePoint,
					 @JsonProperty("runningPoint") Point runningPoint,
					 @JsonProperty("fuelPoint") Point fuelPoint) {
			this.worldName = worldName;
			this.signPoint = signPoint;
			this.wirePoint = wirePoint;
			this.anchorPoint = anchorPoint;
			this.chestPoint = chestPoint;
			this.pausePoint = pausePoint;
			this.runningPoint = runningPoint;
			this.fuelPoint = fuelPoint;

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

	@JsonCreator
	public GeneratorRunePattern(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public GeneratorRunePattern(World world, Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
		model = new Model(world.getName(), s, w, a, c, p, r, f);
	}

	//-----------------------------------------------------------------------

	public World getWorld() {
		return model.world;
	}

	public Set<Point> getPoints() {
		return model.pointsInPattern;
	}

	public Point getAnchorPoint() {
		return model.anchorPoint;
	}

	public Point getSignPoint() {
		return model.signPoint;
	}

	public Point getPausePoint() {
		return model.pausePoint;
	}

	public Point getRunningPoint() {
		return model.runningPoint;
	}

	public Point getFuelPoint() {
		return model.fuelPoint;
	}

	public Point getWirePoint() {
		return model.wirePoint;
	}

	public Point getChestPoint() {
		return model.chestPoint;
	}

	public boolean contains(Point p) {
		return model.pointsInPattern.contains(p);
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
				if (c.is(Material.REDSTONE_WIRE, world) && (w.is(Material.CHEST, world) || w.is(Material.TRAPPED_CHEST, world))) {
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
				valid = valid && (c.is(Material.CHEST, world) || c.is(Material.TRAPPED_CHEST, world));
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
		return s.add(x, y, z);
	}

	public boolean isValid() {
		boolean valid = true;
		valid = valid && model.signPoint.is(Material.LEGACY_WALL_SIGN, model.world);
		valid = valid && model.anchorPoint.is(Material.DIAMOND_BLOCK, model.world);
		valid = valid && model.wirePoint.is(Material.REDSTONE_WIRE, model.world);
		valid = valid && (model.chestPoint.is(Material.CHEST, model.world) || model.chestPoint.is(Material.TRAPPED_CHEST, model.world));

		int goldCount = 0;
		int ironCount = 0;
		Set<Point> points = new HashSet<>();
		points.add(model.pausePoint);
		points.add(model.runningPoint);
		points.add(model.fuelPoint);
		for (Point p : points) {
			if (p.is(Material.IRON_BLOCK, model.world)) {
				ironCount++;
			} else if (p.is(Material.GOLD_BLOCK, model.world)) {
				goldCount++;
			}
		}
		valid = valid && goldCount == 1;
		valid = valid && ironCount == 2;

		return valid;
	}
}
