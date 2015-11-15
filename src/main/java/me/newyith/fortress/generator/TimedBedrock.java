package me.newyith.fortress.generator;

import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

public class TimedBedrock {
	private static Map<Location, TimedBedrockData> dataByLocation = new HashMap<>();

	public static void at(World world, Set<Point> points, int durationTicks) {
		//*
		for (Point p : points) {
			Location loc = p.toLocation(world);
			fromTimedBedrock(loc); //if already timed bedrock, revert now
			toTimedBedrock(loc, durationTicks);
		}
		//*/
	}

	public static Set<Point> getPoints() {
		//*
		Set<Location> locations = dataByLocation.keySet();
		Set<Point> points = new HashSet<>();
		for (Location loc : locations) {
			points.add(new Point(loc));
		}
		return points;
		/*/
		return new HashSet<>();
		//*/
	}

	public static Map<Point, TimedBedrockData> getDataFor(World world, Set<Point> points) {
		Map<Point, TimedBedrockData> map = new HashMap<>();

		//*
		for (Point p : points) {
			Location loc = p.toLocation(world);
			TimedBedrockData data = dataByLocation.get(loc);
			if (data != null) {
				map.put(p, data);
			}
		}
		//*/

		return map;
	}

	public static void abandon(World world, Point p) {
		//*
		dataByLocation.remove(p.toLocation(world));
		//*/
	}

	public static void onTick() {
		//*
//		Debug.start("Bedrock::onTick()");
//		Debug.msg("Bedrock::onTick() dataByLocation.size(): " + dataByLocation.size());
		Iterator<Location> it = dataByLocation.keySet().iterator();
		while (it.hasNext()) {
			Location loc = it.next();
			TimedBedrockData data = dataByLocation.get(loc);
			if (data.waitTicks > 0) {
				data.waitTicks--;
			} else {
				fromTimedBedrock(loc);
				it.remove();
			}
		}
//		Debug.end("Bedrock::onTick()");
		//*/
	}

	private static void toTimedBedrock(Location loc, int durationTicks) {
		Block b = loc.getBlock();
		TimedBedrockData data = new TimedBedrockData(loc, durationTicks);
		dataByLocation.put(loc, data);
		b.setType(Material.QUARTZ_BLOCK); //TODO: change back to BEDROCK
	}

	private static void fromTimedBedrock(Location loc) {
		Point p = new Point(loc);
		boolean altered = FortressesManager.isAltered(p);
		TimedBedrockData data = dataByLocation.get(loc);
		if (data != null && !altered) {
			loc.getBlock().setType(data.material);
		}
	}
}






//	public static void revert(World world, Set<Point> points) {
//		for (Point p : points) {
//			Location loc = p.toLocation(world);
//			fromTimedBedrock(loc);
//			dataByLocation.remove(loc);
//		}
//	}