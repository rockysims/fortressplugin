package me.newyith.fortress.generator.rune;

import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.model.Modelable;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class GeneratorRunePattern implements Modelable {
	public static class Model extends BaseModel {
		String worldName = "";
		Point.Model anchorPoint = null;

		public Model(World world, Point anchorPoint) {
			this.worldName = world.getName();
			this.anchorPoint = anchorPoint.getModel();
		}
	}
	private Model model;
	private World world;
	private Point anchorPoint;

	//TODO: add fields
//	private List<Point> pointsInPattern = new ArrayList<>();
//	public Point anchorPoint = null;
//	public Point pausePoint = null;
//	public Point runningPoint = null;
//	public Point fuelPoint = null;
//	public Point signPoint = null;
//	public Point chestPoint = null;
//	public Point wirePoint = null;

	public GeneratorRunePattern(Model model) {
		this.model = model;
		this.world = Bukkit.getWorld(model.worldName);
		this.anchorPoint = new Point(model.anchorPoint);
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public Point getAnchor() {
		return anchorPoint;
	}

	public World getWorld() {
		return world;
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