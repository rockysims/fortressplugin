package me.newyith.fortresstemp.generator.core;

import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

//TODO: make AwareCore same as core except it should track claims and insideOutside
public abstract class AwareCore extends Core {
	//TODO: rebuild (rather than save/load)
	private Set<Point> claimedPoints = new HashSet<>();
	private Set<Point> layerOutsideFortress = new HashSet<>();
	private Set<Point> pointsInsideFortress = new HashSet<>();

	public AwareCore(Point anchor) {
		super(anchor);
	}
}
