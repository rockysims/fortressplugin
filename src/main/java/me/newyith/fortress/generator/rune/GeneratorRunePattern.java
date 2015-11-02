package me.newyith.fortress.generator.rune;

import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Sign;

import java.util.Set;

public class GeneratorRunePattern {
	private transient World world = null;
	private String worldName = "";
	private Point anchor;

	public World getWorld() {
		if (world == null) {
			world = Bukkit.getWorld(worldName);
		}
		return world;
	}

	//=======================================================================

	private GeneratorRunePattern(World world, Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
//		this.worldName = world.getName();
//		signPoint = s;
//		wirePoint = w;
//		anchorPoint = a;
//		chestPoint = c;
//		pausePoint = p;
//		runningPoint = r;
//		fuelPoint = f;
//
//		//fill pointsInPattern
//		this.pointsInPattern.add(s);
//		this.pointsInPattern.add(w);
//		this.pointsInPattern.add(a);
//		this.pointsInPattern.add(c);
//		this.pointsInPattern.add(p);
//		this.pointsInPattern.add(r);
//		this.pointsInPattern.add(f);
	}

	public Set<Point> getPoints() {
		//TODO: write this method
		return null;
	}

	public Point getAnchor() {
		return anchor;
	}


	public static class Model extends BaseModel {
		//world
		private transient World world = null;
		private String worldName = "";
		public World getWorld() {
			if (world == null) {
				world = Bukkit.getWorld(worldName);
			}
			return world;
		}

		//anchorPoint
		private transient Point anchorPoint = null;
		private Point.Model anchorPointModel = null;
		public Point getAnchorPoint() {
			if (anchorPoint == null) {
				anchorPoint = new Point(anchorPointModel);
			}
			return anchorPoint;
		}


		//TODO: add fields
//	private List<Point> pointsInPattern = new ArrayList<>();
//	public Point anchorPoint = null; //done
//	public Point pausePoint = null;
//	public Point runningPoint = null;
//	public Point fuelPoint = null;
//	public Point signPoint = null;
//	public Point chestPoint = null;
//	public Point wirePoint = null;


		public Model(World world, Point anchorPoint) {
			worldName = world.getName();
			anchorPointModel = anchorPoint.getModel();
		}
	}
	private Model model;

	public GeneratorRunePattern(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------


	private GeneratorRunePattern(Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
//		signPoint = s;
//		wirePoint = w;
//		anchorPoint = a;
//		chestPoint = c;
//		pausePoint = p;
//		runningPoint = r;
//		fuelPoint = f;
//
//		//fill pointsInPattern
//		this.pointsInPattern.add(s);
//		this.pointsInPattern.add(w);
//		this.pointsInPattern.add(a);
//		this.pointsInPattern.add(c);
//		this.pointsInPattern.add(p);
//		this.pointsInPattern.add(r);
//		this.pointsInPattern.add(f);
	}


//	public Point getAnchor() {
//		return model.getAnchorPoint();
//	}
//
//	public World getWorld() {
//		return model.getWorld();
//	}






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
					pattern = new GeneratorRunePattern(s, w, a, c, p, r, f);
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






//	public static FortressGeneratorRunePattern validatePattern(Point signPoint) {
//		return null;
//	}
//
//	public static FortressGeneratorRunePattern tryReadyPattern(Block signBlock) {
//		FortressGeneratorRunePattern pattern = null;
//
//		//if (found sign)
//		if (signBlock.getType() == Material.WALL_SIGN) {
//			Point s = new Point(signBlock.getLocation());
//			Point a = getPointSignAttachedTo(signBlock);
//
//			//if (found anchor)
//			if (a.is(Material.GOLD_BLOCK)) {
//				World world = a.world;
//
//				//set towardFront, towardBack, towardLeft, towardRight
//				Point towardFront = s.difference(a);
//				Point towardLeft = new Point(world, towardFront.z, 0, towardFront.x);
//				if (towardFront.x == 0) {
//					towardLeft = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);
//				}
//				Point towardBack = new Point(world, -1 * towardFront.x, 0, -1 * towardFront.z);
//				Point towardRight = new Point(world, -1 * towardLeft.x, 0, -1 * towardLeft.z);
//
//				//find remaining points
//				Point w = a.add(towardLeft);
//				Point c = a.add(towardRight);
//				Point p = a.add(0, -1, 0).add(towardLeft);
//				Point r = a.add(0, -1, 0);
//				Point f = a.add(0, -1, 0).add(towardRight);
//				if (c.is(Material.REDSTONE_WIRE) && w.is(Material.CHEST)) {
//					Point t;
//					//reverse wire / chest
//					t = w;
//					w = c;
//					c = t;
//					//reverse pause / fuel
//					t = p;
//					p = f;
//					f = t;
//				}
//
//				//check other blocks match pattern
//				boolean valid = true;
//				valid = valid && w.is(Material.REDSTONE_WIRE);
//				valid = valid && c.is(Material.CHEST);
//				valid = valid && p.is(Material.IRON_BLOCK);
//				valid = valid && r.is(Material.DIAMOND_BLOCK);
//				valid = valid && f.is(Material.IRON_BLOCK);
//
//				if (valid) {
//					pattern = new FortressGeneratorRunePattern(s, w, a, c, p, r, f);
//				}
//			}
//		}
//
//		return pattern;
//	}
//
//	private GeneratorRunePattern(Point s, Point w, Point a, Point c, Point p, Point r, Point f) {
//		signPoint = s;
//		wirePoint = w;
//		anchorPoint = a;
//		chestPoint = c;
//		pausePoint = p;
//		runningPoint = r;
//		fuelPoint = f;
//
//		//fill pointsInPattern
//		this.pointsInPattern.add(s);
//		this.pointsInPattern.add(w);
//		this.pointsInPattern.add(a);
//		this.pointsInPattern.add(c);
//		this.pointsInPattern.add(p);
//		this.pointsInPattern.add(r);
//		this.pointsInPattern.add(f);
//	}
//
//	public boolean contains(Block block) {
//		return this.pointsInPattern.contains(new Point(block.getLocation()));
//	}
//
//	public List<Point> getPoints() {
//		return this.pointsInPattern;
//	}










}
