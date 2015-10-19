package me.newyith.fortress.generator2.core;

import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

//TODO: make RichCore same as core except it should track claims and insideOutside
public abstract class RichCore extends Core {
	//TODO: rebuild (rather than save/load)
	private Set<Point> claimedPoints = new HashSet<>();
	private Set<Point> layerOutsideFortress = new HashSet<>();
	private Set<Point> pointsInsideFortress = new HashSet<>();

	public RichCore(Point anchor) {
		super(anchor);
	}
}
